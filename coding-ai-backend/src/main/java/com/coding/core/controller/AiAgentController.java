package com.coding.core.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.coding.core.enums.AgentMessageRoleEnum;
import com.coding.core.enums.AgentMessageTypeEnum;
import com.coding.core.model.vo.AgentMessageVO;
import com.coding.core.model.vo.AgentToolCallVO;
import com.coding.core.manager.AgentManager;
import com.coding.core.model.vo.AgentToolResponseVO;
import com.coding.core.service.ChatConversationService;
import com.coding.core.service.ChatMessageService;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.coding.graph.core.state.OverAllState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI Agent 控制器 TODO 优化代码
 * <p>
 * 提供基于 SSE 的流式接口，支持 React Agent 和 Plan-Execute Agent。
 *
 * @author weilong
 */
@Slf4j
@RestController
@RequestMapping("/ai/agent")
@Tag(name = "Agent Chat服务")
public class AiAgentController {

    @Resource
    private AgentManager agentManager;

    @Resource
    private ChatConversationService conversationService;

    @Resource
    private ChatMessageService chatMessageService;

    private ReactAgent reactAgent;

    private CompiledGraph planExecuteAgent;

    @PostConstruct
    private void init() throws GraphStateException {
        this.reactAgent = agentManager.buildReactAgent();
        this.planExecuteAgent = agentManager.buildPlanExecuteAgent();
    }

    /**
     * React Agent SSE 流式接口
     */
    @GetMapping(value = "/react")
    @Operation(summary = "React Agent服务")
    public SseEmitter streamAgent(@RequestParam("prompt") String prompt, @RequestParam(value = "conversationId", defaultValue = "") String conversationId) throws GraphStateException, GraphRunnerException {
        // 1. 恢复历史对话和追加当前用户消息
        List<Message> messages = conversationService.restoreConversationMessages(conversationId, prompt);

        // 2. 调用 Agent，获取流式结果
        AsyncGenerator<NodeOutput> generator = this.reactAgent.stream(Map.of(
                "messages", messages
        ));

        // 3. 按顺序收集所有消息
        List<Message> allMessages = new ArrayList<>();
        allMessages.add(new UserMessage(prompt));
        // 用于累积当前的文本片段
        StringBuilder currentTextBuilder = new StringBuilder();

        // 4. 创建 SseEmitter，超时时间 5 分钟
        SseEmitter emitter = new SseEmitter(300_000L);

        // 5. 使用 streamForEach 异步处理流式输出
        generator.streamForEach(output -> {
            try {
                if (output == null) {
                    return;
                }

                if (output instanceof StreamingOutput streamingOutput) {
                    // 获取消息内容
                    AssistantMessage message = streamingOutput.getChatResponse().getResult().getOutput();

                    AgentMessageVO messageVO;

                    // 检测是否有工具调用
                    if (CollectionUtil.isNotEmpty(message.getToolCalls())) {
                        // 先保存之前累积的文本
                        if (!currentTextBuilder.isEmpty()) {
                            String remainContent = currentTextBuilder.toString();
                            allMessages.add(new AssistantMessage(remainContent));
                            currentTextBuilder.setLength(0);
                        }

                        // 保存工具调用消息
                        allMessages.add(message);

                        // 发送一份工具调用的数据给前端
                        messageVO = AgentMessageVO.builder()
                                .role(AgentMessageRoleEnum.ASSISTANT.name())
                                .type(AgentMessageTypeEnum.TOOL_CALL.name())
                                .toolCalls(message.getToolCalls().stream()
                                        .map(toolCall -> AgentToolCallVO.builder()
                                                .id(toolCall.id())
                                                .name(toolCall.name())
                                                .arguments(toolCall.arguments())
                                                .build())
                                        .toList())
                                .content(StringUtils.isNotBlank(message.getText()) ? message.getText() : null)
                                .build();
                    } else {
                        // 普通流式文本内容，累积到 StringBuilder 中
                        if (StringUtils.isNotBlank(message.getText())) {
                            currentTextBuilder.append(message.getText());
                        }

                        messageVO = AgentMessageVO.builder()
                                .role(AgentMessageRoleEnum.ASSISTANT.name())
                                .type(AgentMessageTypeEnum.STEP_EXECUTION.name())
                                .content(message.getText())
                                .build();
                    }

                    // 发送消息
                    emitter.send(SseEmitter.event()
                            .data(JSONUtil.toJsonStr(messageVO)));
                }
                // 处理工具节点输出，收集工具响应消息
                else {
                    if ("tool".equals(output.getNode())) {
                        OverAllState toolState = output.getState();

                        if (toolState == null) {
                            return;
                        }
                        toolState.value("messages").ifPresent(messagesObj -> {
                            if (messagesObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Message> ms = (List<Message>) messagesObj;
                                if (CollectionUtil.isEmpty(ms)) {
                                    log.warn("工具节点 messages 列表为空，无法获取工具响应，node: {}", output.getNode());
                                    return;
                                }

                                // 获取最后一条消息（工具响应消息）
                                Message lastMessage = ms.get(ms.size() - 1);
                                if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
                                    // 将工具响应消息添加到收集列表
                                    allMessages.add(toolResponseMessage);
                                    // TODO 发送 工具响应消息
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .data(JSONUtil.toJsonStr(AgentMessageVO.builder()
                                                        .role(AgentMessageRoleEnum.ASSISTANT.name())
                                                        .type(AgentMessageTypeEnum.TOOL_RESPONSE.name())
                                                        .toolResponses(toolResponseMessage.getResponses()
                                                                .stream().map(toolResponse -> AgentToolResponseVO.builder()
                                                                        .id(toolResponse.id())
                                                                        .name(toolResponse.name())
                                                                        .responseData(toolResponse.responseData())
                                                                        .build())
                                                                .toList()))));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        });
                    } else {
                        log.warn("未知类型的输出，result: {}", JSONUtil.toJsonStr(output));
                    }
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
                throw new RuntimeException(e);
            }
        }).exceptionally(error -> {
            // 异常处理
            log.error("React Agent 流处理异常", error);
            emitter.completeWithError(error);
            return null;
        }).thenRun(() -> {
            // 流式处理完毕，保存最后累积的文本
            if (!currentTextBuilder.isEmpty()) {
                allMessages.add(new AssistantMessage(currentTextBuilder.toString()));
            }

            // 保存新的对话记录到数据库
            chatMessageService.saveMessages(conversationId, messages);

            try {
                // 发送结束信号
                emitter.send(SseEmitter.event()
                        .data(JSONUtil.toJsonStr(AgentMessageVO.buildFinishMessage())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 完成流
            emitter.complete();
        });

        return emitter;
    }

    /**
     * Plan-Execute Agent SSE 流式接口
     */
    @GetMapping(value = "/plan-execute")
    @Operation(summary = "Plan-Executing Agent服务")
    public SseEmitter streamPlanExecuteAgent(@RequestParam("prompt") String prompt, @RequestParam(value = "conversationId", defaultValue = "") String conversationId) throws GraphRunnerException {
        // 1. 恢复历史对话和追加当前用户消息
        List<Message> messages = conversationService.restoreConversationMessages(conversationId, prompt);

        // 2. 调用 Plan-Execute Agent，获取流式结果
        AsyncGenerator<NodeOutput> generator = this.planExecuteAgent.stream(Map.of("messages", messages));

        // 3. 按顺序收集所有消息
        List<Message> allMessages = new ArrayList<>();
        allMessages.add(new UserMessage(prompt));
        // 用于累积当前的文本片段
        StringBuilder currentTextBuilder = new StringBuilder();

        // 4. 创建 SseEmitter，超时时间 5 分钟
        SseEmitter emitter = new SseEmitter(300_000L);

        // 5. 使用 streamForEach 异步处理流式输出
        generator.streamForEach(output -> {
            try {
                // 处理 null 输出
                if (output == null || StringUtils.isBlank(output.getNode()) || "preLlm".equals(output.getNode())) {
                    return;
                }

                // 过滤不需要发送给前端的节点（只记录日志）
                String nodeName = output.getNode();
                if ("__END__".equals(nodeName)) {
                    // TODO 发送结束信号
                    return;
                }

                if ("tool".equals(nodeName)) {
                    OverAllState toolState = output.getState();
                    if (toolState == null) {
                        return;
                    }
                    // 收集工具响应消息
                    toolState.value("messages").ifPresent(messagesObj -> {
                        if (messagesObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Message> ms = (List<Message>) messagesObj;
                            if (!ms.isEmpty()) {
                                // 获取最后一条消息（工具响应消息）
                                Message lastMessage = ms.get(ms.size() - 1);
                                if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
                                    // 将工具响应消息添加到收集列表
                                    allMessages.add(toolResponseMessage);
                                    // TODO 发送 工具响应消息
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .data(JSONUtil.toJsonStr(AgentMessageVO.builder()
                                                        .role(AgentMessageRoleEnum.ASSISTANT.name())
                                                        .type(AgentMessageTypeEnum.TOOL_RESPONSE.name())
                                                        .toolResponses(toolResponseMessage.getResponses()
                                                                .stream().map(toolResponse -> AgentToolResponseVO.builder()
                                                                        .id(toolResponse.id())
                                                                        .name(toolResponse.name())
                                                                        .responseData(toolResponse.responseData())
                                                                        .build())
                                                                .toList()))));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            } else {
                                log.warn("messages 列表为空，无法获取工具响应");
                            }
                        } else {
                            log.warn("messages 不是 List 类型，实际类型: {}", messagesObj.getClass());
                        }
                    });
                    return;
                }

                AgentMessageVO messageVO;

                // 处理流式输出
                if (output instanceof StreamingOutput streamingOutput) {
                    AssistantMessage message = streamingOutput.getChatResponse().getResult().getOutput();

                    // 检测是否有工具调用
                    if (CollectionUtil.isNotEmpty(message.getToolCalls())) {
                        // 先保存之前累积的文本（如果有）
                        if (currentTextBuilder.length() > 0) {
                            allMessages.add(new AssistantMessage(currentTextBuilder.toString()));
                            currentTextBuilder.setLength(0); // 清空
                        }

                        // 保存工具调用消息（完整的 AssistantMessage，包含 toolCalls）
                        allMessages.add(message);

                        messageVO = AgentMessageVO.builder()
                                .role(AgentMessageRoleEnum.ASSISTANT.name())
                                .type(AgentMessageTypeEnum.TOOL_CALL.name())
                                .toolCalls(message.getToolCalls().stream()
                                        .map(toolCall -> AgentToolCallVO.builder()
                                                .id(toolCall.id())
                                                .name(toolCall.name())
                                                .arguments(toolCall.arguments())
                                                .build())
                                        .toList())
                                .content(StringUtils.isNotBlank(message.getText()) ? message.getText() : null)
                                .build();
                    } else {
                        // 普通流式文本内容，累积到 StringBuilder 中
                        String content = message.getText();
                        if (content != null && !content.isEmpty()) {
                            currentTextBuilder.append(content);
                        }

                        messageVO = AgentMessageVO.builder()
                                .role(AgentMessageRoleEnum.ASSISTANT.name())
                                .type(AgentMessageTypeEnum.STEP_EXECUTION.name())
                                .content(message.getText())
                                .build();
                    }
                }
                // 处理节点输出（计划和进度）
                else {
                    OverAllState state = output.getState();

                    // 根据不同节点类型，构造不同的输出格式
                    switch (nodeName) {
                        case "planning_agent" -> {
                            // 计划创建完成
                            String planJson = (String) state.value("plan").orElse("");
                            event = AgentMessageVO.builder()
                                    .type("PLAN_CREATED")
                                    .node(nodeName)
                                    .plan(planJson)
                                    .build();

                            log.info("📋 [计划创建] plan: {}", planJson);
                        }
                        case "supervisor_agent" -> {
                            // 计划执行进度
                            AgentMessageVO.PlanExecuteEventVOBuilder builder = AgentMessageVO.builder()
                                    .type("PLAN_PROGRESS")
                                    .node(nodeName);

                            // 提取进度信息
                            state.value("plan_id").ifPresent(v -> builder.planId((String) v));
                            state.value("current_step_index").ifPresent(v -> builder.currentStep((Integer) v));
                            state.value("total_steps").ifPresent(v -> builder.totalSteps((Integer) v));
                            state.value("is_finished").ifPresent(v -> builder.isFinished((Boolean) v));
                            state.value("current_step_description").ifPresent(v -> builder.stepDescription((String) v));
                            state.value("step_status_history").ifPresent(v -> {
                                if (v instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, String> history = (Map<String, String>) v;
                                    builder.history(history);
                                }
                            });

                            // 计算完成百分比
                            if (state.value("current_step_index").isPresent() &&
                                    state.value("total_steps").isPresent()) {
                                int current = (int) state.value("current_step_index").get();
                                int total = (int) state.value("total_steps").get();
                                int percentage = (int) ((current * 100.0) / total);
                                builder.percentage(percentage);
                            }

                            event = builder.build();
                            log.info("📊 [执行进度] currentStep: {}/{}, percentage: {}%",
                                    event.getCurrentStep(), event.getTotalSteps(), event.getPercentage());
                        }
                        case "step_executing_agent" -> {
                            // ✅ 步骤执行完成
                            AgentMessageVO.PlanExecuteEventVOBuilder builder = AgentMessageVO.builder()
                                    .node(nodeName);

                            // 检查是否包含工具返回结果
                            if (state.value("step_output").isPresent()) {
                                Object stepOutput = state.value("step_output").get();
                                String outputStr = stepOutput.toString();

                                // 检测工具返回的特征（URL、文件路径等）
                                boolean isToolResult = outputStr.contains("http") ||
                                        outputStr.contains("URL") ||
                                        outputStr.contains("url") ||
                                        outputStr.contains("生成成功") ||
                                        outputStr.contains("创建完成");

                                if (isToolResult) {
                                    // 🎯 工具返回结果
                                    builder.type("TOOL_RESULT").result(outputStr);
                                    log.info("🎯 [工具返回] result: {}", outputStr);
                                } else {
                                    // 普通步骤完成
                                    builder.type("STEP_COMPLETED").output(outputStr);
                                    log.info("✅ [步骤完成] output: {}", outputStr);
                                }
                            }

                            event = builder.build();
                        }
                        default -> {
                            // 🔧 其他节点输出
                            event = AgentMessageVO.builder()
                                    .type("NODE_OUTPUT")
                                    .node(nodeName)
                                    .data(state.data())
                                    .build();

                            log.info("🔧 [节点输出] node: {}", nodeName);
                        }
                    }
                }

                // 发送事件
                if (event != null) {
                    String jsonContent = JSONUtil.toJsonStr(event);
                    emitter.send(SseEmitter.event()
                            .data(jsonContent)
                            .name("plan-execute"));
                }
            } catch (IOException e) {
                log.error("❌ 发送事件失败", e);
                emitter.completeWithError(e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                log.error("❌ Plan-Execute 流处理异常", e);
                emitter.completeWithError(e);
                throw new RuntimeException(e);
            }
        }).exceptionally(error -> {
            // 异常处理
            log.error("❌ Plan-Execute 流处理异常", error);
            emitter.completeWithError(error);
            return null;
        }).thenRun(() -> {
            // 流式处理完毕，保存最后累积的文本（如果有）
            if (currentTextBuilder.length() > 0) {
                allMessages.add(new AssistantMessage(currentTextBuilder.toString()));
            }

            log.info("✅ Plan-Execute Generator 处理完毕");

            try {
                // 保存完整的会话（用户消息 + 按顺序的所有助手消息）
                if (StringUtils.isNotBlank(cid) && !allMessages.isEmpty()) {
                    List<Message> newMessages = new ArrayList<>();
                    newMessages.add(userMessage);
                    newMessages.addAll(allMessages);

                    CompletableFuture.runAsync(() -> {
                        try {
                            chatMessageService.saveMessages(cid, newMessages);
                            log.info("💾 已保存 Plan-Execute 会话消息，conversationId: {}, 消息数量: {}", cid, newMessages.size());
                        } catch (Exception ex) {
                            log.warn("⚠️ 保存会话消息失败（忽略）", ex);
                        }
                    });
                }

                // 发送结束信号
                AgentMessageVO endEvent = AgentMessageVO.builder()
                        .type("STREAM_END")
                        .message("执行完成")
                        .build();

                emitter.send(SseEmitter.event()
                        .data(JSONUtil.toJsonStr(endEvent))
                        .name("plan-execute"));

                // 完成流
                emitter.complete();
                log.info("✅ Plan-Execute SSE 流已完成");

            } catch (IOException e) {
                log.error("❌ 发送结束信号失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

}
