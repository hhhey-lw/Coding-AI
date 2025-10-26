package com.coding.admin.controller;

import cn.hutool.json.JSONUtil;
import com.coding.admin.model.vo.PlanExecuteEventVO;
import com.coding.admin.manager.AgentManager;
import com.coding.graph.core.agent.ReactAgent;
import com.coding.graph.core.exception.GraphRunnerException;
import com.coding.graph.core.exception.GraphStateException;
import com.coding.graph.core.generator.AsyncGenerator;
import com.coding.graph.core.graph.CompiledGraph;
import com.coding.graph.core.node.NodeOutput;
import com.coding.graph.core.node.StreamingOutput;
import com.coding.graph.core.state.OverAllState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/ai/agent")
public class AiAgentController {

    @Resource
    private AgentManager agentManager;

    private ReactAgent reactAgent;

    private CompiledGraph planExecuteAgent;

    @PostConstruct
    private void init() throws GraphStateException {
        this.reactAgent = agentManager.buildAgent();
        this.planExecuteAgent = agentManager.buildPlanExecuteAgent();
    }

    /**
     * React Agent SSE 流式接口（标准化事件格式）
     * 
     * 支持的事件类型：
     * - STEP_EXECUTION: 流式文本内容
     * - TOOL_CALL: 工具调用
     * - STREAM_END: 流式结束
     */
    @GetMapping(value = "/react")
    public SseEmitter streamAgent(@RequestParam("prompt") String prompt) throws GraphStateException, GraphRunnerException {
        
        log.info("🚀 收到 React Agent SSE 请求，prompt: {}", prompt);
        
        // 创建 SseEmitter，超时时间 5 分钟
        SseEmitter emitter = new SseEmitter(300_000L);
        
        // 调用 Agent，获取流式结果
        AsyncGenerator<NodeOutput> generator = this.reactAgent.stream(Map.of(
                "messages", List.of(new UserMessage(prompt))
        ));
        
        // 🔥 使用独立线程 + 迭代器实现真正的流式处理
        CompletableFuture.runAsync(() -> {
            try {
                log.info("🚀 开始 React Agent 流式处理");
                
                // 使用迭代器逐个处理数据，每产生一个就立即发送一个
                for (NodeOutput output : generator) {
                    if (output instanceof StreamingOutput streamingOutput) {
                        try {
                            // 获取消息内容
                            AssistantMessage message = streamingOutput.getChatResponse().getResult().getOutput();
                            
                            PlanExecuteEventVO event;
                            
                            // 🔧 检测是否有工具调用
                            if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                                // 🛠️ 工具调用事件
                                event = PlanExecuteEventVO.builder()
                                        .type("TOOL_CALL")
                                        .node("react_agent")
                                        .toolCalls(message.getToolCalls().stream()
                                                .map(toolCall -> PlanExecuteEventVO.ToolCallVO.builder()
                                                        .id(toolCall.id())
                                                        .name(toolCall.name())
                                                        .arguments(toolCall.arguments())
                                                        .build())
                                                .toList())
                                        .reasoning(message.getText() != null && !message.getText().isEmpty() ? message.getText() : null)
                                        .build();
                                
                                log.info("🛠️ [React 工具调用] tools: {}", event.getToolCalls().size());
                            } else {
                                // 📝 普通流式内容
                                event = PlanExecuteEventVO.builder()
                                        .type("STEP_EXECUTION")
                                        .node("react_agent")
                                        .content(message.getText())
                                        .build();
                                
                                log.info("📝 [React 执行] content: {}", message.getText());
                            }
                            
                            // 发送事件
                            String jsonContent = JSONUtil.toJsonStr(event);
                            emitter.send(SseEmitter.event()
                                    .data(jsonContent)
                                    .name("react-agent"));

                        } catch (IOException e) {
                            log.error("❌ 发送消息失败", e);
                            emitter.completeWithError(e);
                            return; // 终止处理
                        }
                    }
                }
                
                // 所有消息处理完毕，发送结束信号
                log.info("✅ React Agent Generator 处理完毕");

                try {
                    // 发送结束信号
                    PlanExecuteEventVO endEvent = PlanExecuteEventVO.builder()
                            .type("STREAM_END")
                            .message("执行完成")
                            .build();

                    emitter.send(SseEmitter.event()
                            .data(JSONUtil.toJsonStr(endEvent))
                            .name("react-agent"));

                    log.info("✅ 已发送结束信号");

                    // 完成流
                    emitter.complete();
                    log.info("✅ React Agent SSE 流已完成");

                } catch (IOException e) {
                    log.error("❌ 发送结束信号失败", e);
                    emitter.completeWithError(e);
                }
                
            } catch (Exception e) {
                // 异常处理
                log.error("❌ React Agent 流处理异常", e);
                emitter.completeWithError(e);
            }
        });
        
        // 监听超时和完成事件
        emitter.onTimeout(() -> {
            log.warn("⏰ React Agent SSE 连接超时");
            emitter.complete();
        });
        
        emitter.onCompletion(() -> {
            log.info("🔚 React Agent SSE 连接已关闭");
        });
        
        emitter.onError((e) -> {
            log.error("❌ React Agent SSE 连接错误", e);
        });
        
        return emitter;
    }

    /**
     * Plan-Execute Agent SSE 流式接口
     * 
     * 支持多种事件类型：
     * - PLAN_CREATED: 计划创建完成
     * - PLAN_PROGRESS: 执行进度更新
     * - STEP_EXECUTION: 步骤执行细节（流式）
     * - TOOL_CALL: 工具调用
     * - TOOL_RESULT: 工具返回结果
     * - STEP_COMPLETED: 步骤完成
     * - STREAM_END: 流式结束
     */
    @GetMapping(value = "/plan-execute")
    public SseEmitter streamPlanExecuteAgent(@RequestParam("prompt") String prompt) throws GraphStateException, GraphRunnerException {
        
        log.info("🚀 收到 Plan-Execute SSE 请求，prompt: {}", prompt);
        
        // 创建 SseEmitter，超时时间 10 分钟（Plan-Execute 可能需要更长时间）
        SseEmitter emitter = new SseEmitter(600_000L);
        
        // 调用 Plan-Execute Agent，获取流式结果
        AsyncGenerator<NodeOutput> generator = this.planExecuteAgent.stream(Map.of("input", prompt));
        
        // 🔥 使用独立线程 + 迭代器实现真正的流式处理
        CompletableFuture.runAsync(() -> {
            try {
                log.info("🚀 开始 Plan-Execute 流式处理");
                
                // 使用迭代器逐个处理数据，每产生一个就立即发送一个
                for (NodeOutput output : generator) {
                    // 处理 null 输出
                    if (output == null) {
                        continue;
                    }
                    
                    String nodeName = output.getNode();
                    if (nodeName == null || nodeName.isEmpty()) {
                        log.warn("⚠️ 节点名称为空，跳过");
                        continue;
                    }
                    
                    // 🔇 过滤不需要发送给前端的节点（只记录日志）
                    if ("__END__".equals(nodeName)) {
                        log.debug("🔇 [结束节点] node: __END__, state: {}", output.getState() != null ? output.getState().data() : "null");
                        continue;
                    }
                    
                    if ("preLlm".equals(nodeName)) {
                        log.debug("🔇 [预处理节点] node: preLlm, 仅记录日志，不发送给前端");
                        continue;
                    }
                    
                    // 🔇 tool 节点通常只包含中间状态，工具结果会在后续节点体现
                    // 这里可以记录详细日志，但不发送给前端（避免冗余数据）
                    if ("tool".equals(nodeName)) {
                        OverAllState toolState = output.getState();
                        if (toolState != null) {
                            log.debug("🔧 [工具节点] node: tool, messages count: {}", 
                                toolState.value("messages").map(m -> m instanceof List ? ((List<?>) m).size() : 0).orElse(0));
                        }
                        continue;
                    }
                    
                    PlanExecuteEventVO event;
                    
                    // 🔥 处理流式输出（React Agent 执行细节）
                    if (output instanceof StreamingOutput streamingOutput) {
                        AssistantMessage message = streamingOutput.getChatResponse().getResult().getOutput();
                        
                        // 🔧 检测是否有工具调用
                        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                            // 🛠️ 工具调用事件
                            event = PlanExecuteEventVO.builder()
                                    .type("TOOL_CALL")
                                    .node(nodeName)
                                    .toolCalls(message.getToolCalls().stream()
                                            .map(toolCall -> PlanExecuteEventVO.ToolCallVO.builder()
                                                    .id(toolCall.id())
                                                    .name(toolCall.name())
                                                    .arguments(toolCall.arguments())
                                                    .build())
                                            .toList())
                                    .reasoning(message.getText() != null && !message.getText().isEmpty() ? message.getText() : null)
                                    .build();
                            
                            log.info("🛠️ [工具调用] node: {}, tools: {}", nodeName, event.getToolCalls().size());
                        } else {
                            // 📝 普通流式内容
                            event = PlanExecuteEventVO.builder()
                                    .type("STEP_EXECUTION")
                                    .node(nodeName)
                                    .content(message.getText())
                                    .build();
                            
                            log.info("📝 [执行细节] node: {}, content: {}", nodeName, message.getText());
                        }
                    }
                    // 🔥 处理节点输出（计划和进度）
                    else {
                        OverAllState state = output.getState();
                        
                        // 根据不同节点类型，构造不同的输出格式
                        switch (nodeName) {
                            case "planning_agent" -> {
                                // 📋 计划创建完成
                                String planJson = (String) state.value("plan").orElse("");
                                event = PlanExecuteEventVO.builder()
                                        .type("PLAN_CREATED")
                                        .node(nodeName)
                                        .plan(planJson)
                                        .build();
                                
                                log.info("📋 [计划创建] plan: {}", planJson);
                            }
                            case "supervisor_agent" -> {
                                // 📊 计划执行进度
                                PlanExecuteEventVO.PlanExecuteEventVOBuilder builder = PlanExecuteEventVO.builder()
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
                                PlanExecuteEventVO.PlanExecuteEventVOBuilder builder = PlanExecuteEventVO.builder()
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
                                event = PlanExecuteEventVO.builder()
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
                        try {
                            String jsonContent = JSONUtil.toJsonStr(event);
                            emitter.send(SseEmitter.event()
                                    .data(jsonContent)
                                    .name("plan-execute"));
                        } catch (IOException e) {
                            log.error("❌ 发送事件失败", e);
                            emitter.completeWithError(e);
                            return; // 终止处理
                        }
                    }
                }
                
                // 所有消息处理完毕，发送结束信号
                log.info("✅ Plan-Execute Generator 处理完毕");
                
                try {
                    PlanExecuteEventVO endEvent = PlanExecuteEventVO.builder()
                            .type("STREAM_END")
                            .message("执行完成")
                            .build();
                    
                    emitter.send(SseEmitter.event()
                            .data(JSONUtil.toJsonStr(endEvent))
                            .name("plan-execute"));
                    
                    log.info("✅ 已发送结束信号");
                    
                    // 完成流
                    emitter.complete();
                    log.info("✅ Plan-Execute SSE 流已完成");
                    
                } catch (IOException e) {
                    log.error("❌ 发送结束信号失败", e);
                    emitter.completeWithError(e);
                }
                
            } catch (Exception e) {
                // 异常处理
                log.error("❌ Plan-Execute 流处理异常", e);
                emitter.completeWithError(e);
            }
        });
        
        // 监听超时和完成事件
        emitter.onTimeout(() -> {
            log.warn("⏰ Plan-Execute SSE 连接超时");
            emitter.complete();
        });
        
        emitter.onCompletion(() -> {
            log.info("🔚 Plan-Execute SSE 连接已关闭");
        });
        
        emitter.onError((e) -> {
            log.error("❌ Plan-Execute SSE 连接错误", e);
        });
        
        return emitter;
    }

}
