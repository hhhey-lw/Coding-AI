package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.agentflow.service.tool.ToolManager;
import com.coding.graph.core.state.OverAllState;
import com.coding.core.model.entity.KnowledgeVectorDO;
import com.coding.core.repository.KnowledgeVectorRepository;
import com.coding.graph.core.streaming.StreamingChatGenerator;
import com.coding.workflow.utils.AssertUtil;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
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

    public AgentNode(@Value("${spring.ai.openai.embedding.options.dimensions:1024}") Integer defaultDimensions,
                     ChatModel chatModel,
                     EmbeddingModel embeddingModel,
                     KnowledgeVectorRepository knowledgeVectorRepository,
                     ToolManager toolManager) {
        this.DEFAULT_DIMENSIONS = defaultDimensions;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.knowledgeVectorRepository = knowledgeVectorRepository;
        this.toolManager = toolManager;
    }

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) throws Exception {
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

        log.info("执行Agent节点，模型: {}, 知识库: {}, 工具: {}", chatModel, knowledgeBaseIds, tools);

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
        String ragContext = retrieveRagContext(knowledgeBaseIds, embeddingModelName, rerankModelName, userQuery, topK);
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

        // 构建ChatClient - 工具调用 & 思考内容
        ChatClient.ChatClientRequestSpec chatRequest = buildChatRequest(this.chatModel, chatModelName, tools, springMessages);

        // Agent通常会进行多轮思考和工具调用
        if (isStream) {
            Flux<ChatResponse> chatResponseFlux = chatRequest.stream().chatResponse();
            // 使用 StreamingChatGenerator 包装流式响应
            var generator = StreamingChatGenerator.builder()
                    .startingNode(node.getId())
                    .startingState(state)
                    .mapResult(response -> {
                        // mapResult 在流式结束时调用，返回最终完整的结果
                        String output = response.getResult().getOutput().getText();
                        Map<String, Object> usage = extractUsage(response.getMetadata());

                        Map<String, Object> finalResult = new HashMap<>();
                        finalResult.put("model", chatModelName);
                        finalResult.put("output", output);
                        finalResult.put("usage", usage);
                        return finalResult;
                    })
                    .build(chatResponseFlux);

            // 框架会自动识别 AsyncGenerator 并处理流式输出
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("output", generator);

            return resultData;

        } else {
            String output = chatRequest.call().content();

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
        String chatModel = getConfigParamAsString(node, "model", "qwen-plus");
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
     * 构建Chat请求
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
            List<KnowledgeVectorDO> allRetrievedDocs = new ArrayList<>();
            for (String knowledgeBaseIdStr : knowledgeBaseIds) {
                try {
                    Long knowledgeBaseId = Long.parseLong(knowledgeBaseIdStr);
                    List<KnowledgeVectorDO> docs = knowledgeVectorRepository.similaritySearch(
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
            List<KnowledgeVectorDO> topDocs = allRetrievedDocs.stream()
                    .sorted(Comparator.comparing(KnowledgeVectorDO::getSimilarity).reversed())
                    .limit(topK)
                    .toList();

            // 5. 构建RAG上下文
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("以下是从知识库中检索到的相关信息，请参考这些信息来回答用户的问题：\n\n");

            for (int i = 0; i < topDocs.size(); i++) {
                KnowledgeVectorDO doc = topDocs.get(i);
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
