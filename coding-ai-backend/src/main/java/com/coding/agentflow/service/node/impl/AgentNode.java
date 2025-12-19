package com.coding.agentflow.service.node.impl;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.agentflow.service.node.AbstractNode;
import com.coding.agentflow.service.tool.ToolManager;
import com.coding.agentflow.utils.StreamingToolCallMerger;
import com.coding.core.model.model.KnowledgeVectorModel;
import com.coding.graph.core.state.OverAllState;
import com.coding.core.repository.KnowledgeVectorRepository;
import com.coding.graph.core.generator.streaming.StreamingChatGenerator;
import com.coding.workflow.utils.AssertUtil;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Agent节点
 * 执行具有自主决策能力的智能代理任务
 */
@Slf4j
@Component
public class AgentNode extends AbstractNode {

    private final Integer DEFAULT_DIMENSIONS;

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final KnowledgeVectorRepository knowledgeVectorRepository;
    private final ToolManager toolManager;
    private final ChatMemory chatMemory;

    public AgentNode(@Value("${spring.ai.openai.embedding.options.dimensions:1024}") Integer defaultDimensions,
                     ChatModel chatModel,
                     EmbeddingModel embeddingModel,
                     KnowledgeVectorRepository knowledgeVectorRepository,
                     ToolManager toolManager,
                     ChatMemory chatMemory) {
        this.DEFAULT_DIMENSIONS = defaultDimensions;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.knowledgeVectorRepository = knowledgeVectorRepository;
        this.toolManager = toolManager;
        this.chatMemory = chatMemory;
    }

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("[AgentNode] 开始执行, nodeId={}", node.getId());

        // 获取配置参数 - 需要支持ToolCall
        String chatModelName = getConfigParamAsString(node, "chatModel", "qwen-plus");
        Boolean isStream = getConfigParamAsBoolean(node, "isStream", Boolean.TRUE);
        // 知识库相关参数
        List<String> knowledgeBaseIds = getConfigParamAsList(node, "knowledgeBaseIds");
        Integer topK = getConfigParamAsInteger(node, "topK", 5);
        String embeddingModelName = getConfigParamAsString(node, "embeddingModel", "");
        String rerankModelName = getConfigParamAsString(node, "rerankModel", "");
        // 工具名称列表
        List<String> tools = getConfigParamAsList(node, "tools");
        // Memory 相关配置
        Boolean enableMemory = getConfigParamAsBoolean(node, "enableMemory", false);
        Integer memorySize = getConfigParamAsInteger(node, "memorySize", 100);
        // conversationId 从 state 获取，用于区分不同会话
        String conversationId = state.value("conversationId", "default-" + node.getId());

        log.info("[AgentNode] 配置解析完成, 模型: {}, 知识库: {}, 工具: {}, Memory: {}, 耗时: {}ms",
                chatModelName, knowledgeBaseIds, tools, enableMemory, System.currentTimeMillis() - startTime);

        List<Message> springMessages = new ArrayList<>();
        Object messagesObj = node.getConfigParams().get("messages");
        String userQuery = "";

        // 1. 提取用户查询用于RAG (取最后一个 User 消息)
        if (messagesObj instanceof List) {
            List<Map<String, String>> messagesConfig = (List<Map<String, String>>) messagesObj;
            for (int i = messagesConfig.size() - 1; i >= 0; i--) {
                Map<String, String> msgMap = messagesConfig.get(i);
                if ("user".equalsIgnoreCase(msgMap.get("role"))) {
                    String content = msgMap.get("content");
                    userQuery = replaceTemplateWithVariable(content, state);
                    break;
                }
            }
        }
        AssertUtil.isNotBlank(userQuery, "Agent节点的用户提示词不能为空！");

        // 2. 检索知识库并构建RAG上下文
        long ragStartTime = System.currentTimeMillis();
        String ragContext = retrieveRagContext(knowledgeBaseIds, embeddingModelName, rerankModelName, userQuery, topK);
        log.info("[AgentNode] RAG检索完成, 上下文长度: {}, 耗时: {}ms",
                ragContext.length(), System.currentTimeMillis() - ragStartTime);
        boolean ragConsumed = false;
        boolean hasSystem = false;

        // 3. 构建消息列表并注入RAG上下文
        if (messagesObj instanceof List) {
            List<Map<String, String>> messagesConfig = (List<Map<String, String>>) messagesObj;
            for (Map<String, String> msgMap : messagesConfig) {
                String role = msgMap.get("role");
                String content = msgMap.get("content");
                String finalContent = replaceTemplateWithVariable(content, state);

                if ("system".equalsIgnoreCase(role)) {
                    if (StringUtils.isNotBlank(ragContext) && !ragConsumed) {
                        finalContent = finalContent + "\n\n" + ragContext;
                        ragConsumed = true;
                    }
                    springMessages.add(new SystemMessage(finalContent));
                    hasSystem = true;
                } else if ("user".equalsIgnoreCase(role)) {
                    springMessages.add(new UserMessage(finalContent));
                } else if ("assistant".equalsIgnoreCase(role)) {
                    springMessages.add(new AssistantMessage(finalContent));
                }
            }
        }

        // 4. 如果 RAG 上下文未被注入（没有系统消息），或者需要默认系统消息
        if (StringUtils.isNotBlank(ragContext) && !ragConsumed) {
            springMessages.add(0, new SystemMessage(ragContext));
            hasSystem = true;
        }

        if (!hasSystem) {
            springMessages.add(0, new SystemMessage("你是一个乐于助人的AI助手，能够帮助用户完成任务。"));
        }

        // 5. 如果启用 Memory，读取历史消息并插入到消息列表中
        final String finalConversationId = conversationId;
        final String finalUserQuery = userQuery;
        if (Boolean.TRUE.equals(enableMemory)) {
            List<Message> historyMessages = chatMemory.get(conversationId);
            if (historyMessages != null && !historyMessages.isEmpty()) {
                // 限制历史消息数量
                int historyCount = Math.min(historyMessages.size(), memorySize);
                List<Message> limitedHistory = historyMessages.subList(
                        historyMessages.size() - historyCount, historyMessages.size());
                // 将历史消息插入到 system 消息之后，当前消息之前
                int insertIndex = 1; // system 消息在索引 0
                springMessages.addAll(insertIndex, limitedHistory);
                log.info("[AgentNode] 加载历史消息, conversationId={}, 历史条数: {}", conversationId, limitedHistory.size());
            }
        }

        // 获取工具回调
        long toolLoadStartTime = System.currentTimeMillis();
        List<ToolCallback> toolCallbacks = toolManager.getToolCallbacks(Sets.newHashSet(tools));
        Map<String, ToolCallback> toolCallbackMap = new HashMap<>();
        for (ToolCallback tc : toolCallbacks) {
            toolCallbackMap.put(tc.getToolDefinition().name(), tc);
        }
        log.info("[AgentNode] 工具加载完成, 工具数量: {}, 耗时: {}ms",
                toolCallbacks.size(), System.currentTimeMillis() - toolLoadStartTime);

        log.info("[AgentNode] 准备阶段完成, 总耗时: {}ms, 开始{}调用LLM",
                System.currentTimeMillis() - startTime, isStream ? "流式" : "同步");

        // Agent通常会进行多轮思考和工具调用
        if (isStream) {
            // 使用手动 tool call 处理模式，解决 Qwen 等 API 的流式 tool call 合并问题
            Flux<ChatResponse> chatResponseFlux = executeStreamingWithToolCalls(
                    springMessages, chatModelName, toolCallbacks, toolCallbackMap);
            // 使用 StreamingChatGenerator 包装流式响应
            var generator = StreamingChatGenerator.builder()
                    .startingNode(node.getId())
                    .startingState(state)
                    .mapResult(response -> {
                        // mapResult 在流式结束时调用，返回最终完整的结果
                        String output = response.getResult().getOutput().getText();
                        Map<String, Object> usage = extractUsage(response.getMetadata());

                        // 如果启用 Memory，保存当前轮次的对话到 Memory
                        if (Boolean.TRUE.equals(enableMemory)) {
                            try {
                                chatMemory.add(finalConversationId, new UserMessage(finalUserQuery));
                                chatMemory.add(finalConversationId, new AssistantMessage(output));
                                log.info("[AgentNode] 保存对话到 Memory, conversationId={}", finalConversationId);
                            } catch (Exception e) {
                                log.error("[AgentNode] 保存 Memory 失败", e);
                            }
                        }

                        // 注意：由于是嵌套的，因此这个不会追加命名空间
                        // 这里手动补充
                        Map<String, Object> finalResult = new HashMap<>();
                        finalResult.put(node.getId() + ".model", chatModelName);
                        finalResult.put(node.getId() + ".output", output);
                        finalResult.put(node.getId() + ".usage", usage);
                        return finalResult;
                    })
                    .build(chatResponseFlux);

            // 框架会自动识别 AsyncGenerator 并处理流式输出
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("output", generator);

            return resultData;

        } else {
            // 非流式模式使用 ChatClient（内部 tool 执行）
            ChatClient.ChatClientRequestSpec chatRequest = buildChatRequest(this.chatModel, chatModelName, tools, springMessages);
            String output = chatRequest.call().content();

            // 如果启用 Memory，保存当前轮次的对话到 Memory
            if (Boolean.TRUE.equals(enableMemory)) {
                try {
                    chatMemory.add(finalConversationId, new UserMessage(finalUserQuery));
                    chatMemory.add(finalConversationId, new AssistantMessage(output));
                    log.info("[AgentNode] 保存对话到 Memory, conversationId={}", finalConversationId);
                } catch (Exception e) {
                    log.error("[AgentNode] 保存 Memory 失败", e);
                }
            }

            // 包装返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("reasoning", "Agent推理过程（待实现）");
            resultData.put("output", output);

            return resultData;
        }
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String chatModel = getConfigParamAsString(node, "model", "qwen-plus-latest");
        if (chatModel == null || chatModel.isEmpty()) {
            log.error("Agent节点缺少必需的chatModel配置");
            return false;
        }
        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.AGENT.name();
    }

    /**
     * 执行流式对话，手动处理 tool call 合并和执行。
     * 解决 Qwen 等 OpenAI 兼容 API 的流式 tool call 合并问题。
     *
     * 策略：先聚合检测是否有 tool call，如果有则执行后递归；如果没有则重新发起流式请求返回。
     */
    private Flux<ChatResponse> executeStreamingWithToolCalls(
            List<Message> messages,
            String chatModelName,
            List<ToolCallback> toolCallbacks,
            Map<String, ToolCallback> toolCallbackMap) {

        final long llmStartTime = System.currentTimeMillis();
        final int round = countToolResponseMessages(messages) + 1;
        log.info("[AgentNode] 第{}轮LLM调用开始, 消息数量: {}", round, messages.size());

        // 构建 ChatOptions，禁用内部 tool 执行
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(chatModelName)
                .internalToolExecutionEnabled(false)  // 禁用内部 tool 执行，手动处理
                .streamUsage(true)
                .parallelToolCalls(true)
                .toolCallbacks(toolCallbacks)
                .build();

        Prompt prompt = new Prompt(messages, options);

        // 获取流式响应
        Flux<ChatResponse> streamResponse = chatModel.stream(prompt);

        // 使用 streamAndAggregate：边流式输出边聚合，最后追加聚合结果
        // 这样用户可以看到 AI 的思考过程（如 "让我帮你查一下..."）
        return StreamingToolCallMerger.streamAndAggregate(streamResponse)
                .flatMap(response -> {
                    // 检查是否是聚合结果（最后一个特殊响应）
                    if (StreamingToolCallMerger.isAggregatedResponse(response)) {
                        log.info("[AgentNode] 第{}轮LLM响应聚合完成, 耗时: {}ms",
                                round, System.currentTimeMillis() - llmStartTime);

                        // 检查是否有 tool calls 需要执行
                        if (response.getResult() != null
                                && response.getResult().getOutput() != null
                                && response.getResult().getOutput().hasToolCalls()) {

                            int toolCallCount = response.getResult().getOutput().getToolCalls().size();
                            log.info("[AgentNode] 第{}轮检测到{}个tool calls, 准备执行", round, toolCallCount);

                            // 执行 tool calls 并继续对话
                            return executeToolCallsAndContinue(
                                    messages, response, chatModelName,
                                    toolCallbacks, toolCallbackMap);
                        }
                        // 没有 tool calls，不再发送聚合结果（前面已经流式输出了）
                        log.info("[AgentNode] 第{}轮无tool calls, 流式输出完成", round);
                        return Flux.empty();
                    }
                    // 普通 chunk，直接透传给前端
                    return Flux.just(response);
                });
    }

    /**
     * 统计消息列表中 ToolResponseMessage 的数量（用于计算轮次）
     */
    private int countToolResponseMessages(List<Message> messages) {
        return (int) messages.stream()
                .filter(m -> m instanceof ToolResponseMessage)
                .count();
    }

    /**
     * 执行 tool calls 并继续对话
     */
    private Flux<ChatResponse> executeToolCallsAndContinue(
            List<Message> originalMessages,
            ChatResponse toolCallResponse,
            String chatModelName,
            List<ToolCallback> toolCallbacks,
            Map<String, ToolCallback> toolCallbackMap) {

        AssistantMessage assistantMessage = toolCallResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

        // 提取工具名称列表
        List<String> toolNames = toolCalls.stream()
                .map(AssistantMessage.ToolCall::name)
                .toList();

        // 创建工具调用开始的状态消息（JSON格式，前端可解析）
        Flux<ChatResponse> startFlux = Flux.just(createToolStatusResponse("tool_call_start", toolNames, 0));

        // 使用 Flux.defer 延迟执行工具调用，确保状态消息先发出
        Flux<ChatResponse> toolExecutionFlux = Flux.defer(() -> {
            long toolExecStartTime = System.currentTimeMillis();
            log.info("[AgentNode] 开始执行 {} 个 tool calls", toolCalls.size());

            // 执行所有 tool calls
            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

            for (int i = 0; i < toolCalls.size(); i++) {
                AssistantMessage.ToolCall toolCall = toolCalls.get(i);
                String toolName = toolCall.name();
                String toolArgs = toolCall.arguments();
                String toolCallId = toolCall.id();

                long singleToolStartTime = System.currentTimeMillis();
                log.info("[AgentNode] 执行工具 [{}/{}]: {}, 参数: {}", i + 1, toolCalls.size(), toolName, toolArgs);

                ToolCallback callback = toolCallbackMap.get(toolName);
                if (callback != null) {
                    try {
                        String result = callback.call(toolArgs);
                        long toolDuration = System.currentTimeMillis() - singleToolStartTime;
                        log.info("[AgentNode] 工具 {} 执行成功, 耗时: {}ms, 结果长度: {}",
                                toolName, toolDuration, result != null ? result.length() : 0);
                        toolResponses.add(new ToolResponseMessage.ToolResponse(toolCallId, toolName, result));
                    } catch (Exception e) {
                        long toolDuration = System.currentTimeMillis() - singleToolStartTime;
                        log.error("[AgentNode] 工具 {} 执行失败, 耗时: {}ms", toolName, toolDuration, e);
                        toolResponses.add(new ToolResponseMessage.ToolResponse(
                                toolCallId, toolName, "工具执行失败: " + e.getMessage()));
                    }
                } else {
                    log.warn("[AgentNode] 未找到工具: {}", toolName);
                    toolResponses.add(new ToolResponseMessage.ToolResponse(
                            toolCallId, toolName, "未找到工具: " + toolName));
                }
            }

            long totalDuration = System.currentTimeMillis() - toolExecStartTime;
            log.info("[AgentNode] 所有工具执行完成, 总耗时: {}ms", totalDuration);

            // 创建工具调用完成的状态消息
            Flux<ChatResponse> endFlux = Flux.just(createToolStatusResponse("tool_call_end", toolNames, totalDuration));

            // 构建新的消息列表，包含 assistant message 和 tool responses
            List<Message> newMessages = new ArrayList<>(originalMessages);
            newMessages.add(assistantMessage);
            newMessages.add(ToolResponseMessage.builder().responses(toolResponses).build());

            // 先发送完成状态，再递归调用继续对话
            return endFlux.concatWith(
                    executeStreamingWithToolCalls(newMessages, chatModelName, toolCallbacks, toolCallbackMap));
        });

        // 先发送开始状态消息，再执行工具调用
        return startFlux.concatWith(toolExecutionFlux);
    }

    /**
     * 创建工具调用状态的 ChatResponse（JSON格式，前端可解析做特殊处理）
     *
     * @param type 状态类型：tool_call_start 或 tool_call_end
     * @param tools 工具名称列表
     * @param duration 执行耗时（毫秒），仅在 tool_call_end 时有效
     */
    private ChatResponse createToolStatusResponse(String type, List<String> tools, long duration) {
        // 构建 JSON 格式的状态消息
        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"").append(type).append("\",");
        json.append("\"tools\":[");
        for (int i = 0; i < tools.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(tools.get(i)).append("\"");
        }
        json.append("]");
        if ("tool_call_end".equals(type)) {
            json.append(",\"duration\":").append(duration);
        }
        json.append("}");

        // 使用特殊的 metadata 标记这是工具状态消息
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("__tool_status__", true);
        metadata.put("__tool_status_type__", type);

        AssistantMessage statusMessage = AssistantMessage.builder()
                .content(json.toString())
                .properties(metadata)
                .build();
        Generation generation = new Generation(statusMessage);
        return ChatResponse.builder()
                .generations(List.of(generation))
                .build();
    }

    /**
     * 构建Chat请求（用于非流式模式）
     */
    private ChatClient.ChatClientRequestSpec buildChatRequest(ChatModel chatModel, String chatModelName, List<String> tools, List<Message> messages) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // 使用 ToolManager 获取工具回调
        List<ToolCallback> toolCallbacks = toolManager.getToolCallbacks(Sets.newHashSet(tools));

        return chatClient
                .prompt()
                .messages(messages)
                .options(OpenAiChatOptions.builder()
                        .model(chatModelName)
                        .internalToolExecutionEnabled(true)
                        .streamUsage(true)
                        .parallelToolCalls(true)
                        .build())
                .toolCallbacks(toolCallbacks);
    }


    /**
     * 检索模型知识库并构建RAG上下文
     */
    private String retrieveRagContext(List<String> knowledgeBaseIds, String embeddingModelName, String rerankModelName, String finalUserPrompt, Integer topK) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty() || StringUtils.isBlank(finalUserPrompt)) {
            return "";
        }

        try {
            // 1. 使用嵌入模型将用户查询转换为向量
            EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(finalUserPrompt), OpenAiEmbeddingOptions.builder()
                    .model(embeddingModelName)
                    .dimensions(DEFAULT_DIMENSIONS)
                    .build());
            EmbeddingResponse embeddingResponse = embeddingModel.call(embeddingRequest);
            if (embeddingResponse == null || embeddingResponse.getResults().isEmpty()) {
                log.warn("嵌入模型返回空结果");
                return StringUtils.EMPTY;
            }

            // 获取查询向量
            float[] queryEmbedding = embeddingResponse.getResult().getOutput();
            StringBuilder embeddingBuilder = new StringBuilder("[");
            for (int i = 0; i < queryEmbedding.length; i++) {
                if (i > 0) embeddingBuilder.append(",");
                embeddingBuilder.append(queryEmbedding[i]);
            }
            embeddingBuilder.append("]");
            String embeddingStr = embeddingBuilder.toString();

            // 2. 从每个知识库中检索相关文档
            List<KnowledgeVectorModel> allRetrievedDocs = new ArrayList<>();
            for (String knowledgeBaseIdStr : knowledgeBaseIds) {
                try {
                    Long knowledgeBaseId = Long.parseLong(knowledgeBaseIdStr);
                    List<KnowledgeVectorModel> docs = knowledgeVectorRepository.similaritySearch(
                            knowledgeBaseId, embeddingStr, topK);
                    if (docs != null && !docs.isEmpty()) {
                        allRetrievedDocs.addAll(docs);
                    }
                } catch (NumberFormatException e) {
                    log.error("知识库ID格式错误: {}", knowledgeBaseIdStr, e);
                }
            }

            // 3. 如果没有检索到文档
            if (allRetrievedDocs.isEmpty()) {
                return StringUtils.EMPTY;
            }

            // 4. TODO: 重排序 (rerank)
            List<KnowledgeVectorModel> topDocs = allRetrievedDocs.stream()
                    .sorted(Comparator.comparing(KnowledgeVectorModel::getSimilarity).reversed())
                    .limit(topK)
                    .toList();

            // 5. 构建RAG上下文
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("以下是从知识库中检索到的相关信息，请参考这些信息来回答用户的问题：\n\n");

            for (int i = 0; i < topDocs.size(); i++) {
                KnowledgeVectorModel doc = topDocs.get(i);
                contextBuilder.append("[文档 ").append(i + 1).append("]\n");
                contextBuilder.append(doc.getContent()).append("\n\n");
            }

            contextBuilder.append("请基于以上知识库内容，结合你的知识，准确、专业地回答用户的问题。 如果知识库内容不足以回答问题，可以说明无法回答。\n");

            return contextBuilder.toString();

        } catch (Exception e) {
            log.error("查询知识库失败", e);
            return StringUtils.EMPTY;
        }
    }

    /**
     * 提取token使用情况
     */
    private Map<String, Object> extractUsage(ChatResponseMetadata metadata) {
        Map<String, Object> usage = new HashMap<>();

        if (metadata != null && metadata.getUsage() != null) {
            usage.put("promptTokens", metadata.getUsage().getPromptTokens() != null ? metadata.getUsage().getPromptTokens() : 0);
            usage.put("completionTokens", metadata.getUsage().getCompletionTokens() != null ? metadata.getUsage().getCompletionTokens() : 0);
            usage.put("totalTokens", metadata.getUsage().getTotalTokens() != null ? metadata.getUsage().getTotalTokens() : 0);
        } else {
            usage.put("promptTokens", 0);
            usage.put("completionTokens", 0);
            usage.put("totalTokens", 0);
        }

        return usage;
    }

}
