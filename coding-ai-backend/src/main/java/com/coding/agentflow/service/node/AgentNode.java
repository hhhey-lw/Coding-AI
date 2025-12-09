package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;
import com.coding.core.manager.tool.ImageGenerateService;
import com.coding.core.manager.tool.MusicGenerateService;
import com.coding.core.model.entity.KnowledgeVectorDO;
import com.coding.core.repository.KnowledgeVectorRepository;
import com.coding.workflow.utils.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private final MusicGenerateService musicGenerateService;
    private final ImageGenerateService imageGenerateService;

    public AgentNode(@Value("${spring.ai.openai.embedding.options.dimensions:1024}") Integer defaultDimensions,
                     ChatModel chatModel,
                     EmbeddingModel embeddingModel,
                     KnowledgeVectorRepository knowledgeVectorRepository,
                     MusicGenerateService musicGenerateService,
                     ImageGenerateService imageGenerateService) {
        this.DEFAULT_DIMENSIONS = defaultDimensions;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.knowledgeVectorRepository = knowledgeVectorRepository;
        this.musicGenerateService = musicGenerateService;
        this.imageGenerateService = imageGenerateService;
    }

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) throws Exception {
        // 获取配置参数 - 需要支持ToolCall
        String chatModelName = getConfigParamAsString(node, "chatModel", "qwen-plus");
        // 知识库相关参数
        List<String> knowledgeBaseIds = getConfigParamAsList(node, "knowledgeBaseIds");
        Integer topK = getConfigParamAsInteger(node, "topK", 5);
        String embeddingModelName = getConfigParamAsString(node, "embeddingModel", "");
        String rerankModelName = getConfigParamAsString(node, "rerankModel", "");
        // 工具参数
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
        String output = chatRequest.call().content();

        // 包装返回结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("reasoning", "Agent推理过程（待实现）");
        resultData.put("answer", output);

        return resultData;
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

        List<ToolCallback> toolCallbacks = buildToolCallbacks(tools);

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
                return "你是一个乐于助人的AI助手，能够帮助用户完成任务。";
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
                return "你是一个乐于助人的AI助手，能够帮助用户完成任务。";
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
            return "你是一个乐于助人的AI助手，能够帮助用户完成任务。";
        }
    }

    /**
     * 构建工具回调列表
     *
     * @param tools 工具名称列表
     * @return 工具回调列表
     */
    private List<ToolCallback> buildToolCallbacks(List<String> tools) {
        if (tools == null || tools.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (String toolName : tools) {
            try {
                switch (toolName) {
                    case "generateMusic":
                        toolCallbacks.add(
                                FunctionToolCallback.builder("generateMusic", musicGenerateService)
                                        .description("根据风格提示词和歌词内容，生成一段音乐，并返回音乐的URL地址")
                                        .inputType(MusicGenerateService.Request.class)
                                        .build()
                        );
                        break;
                    case "generateImage":
                        toolCallbacks.add(
                                FunctionToolCallback.builder("generateImage", imageGenerateService)
                                        .description("根据图片提示词和参考图生成对应的图片，并返回图片的URL地址")
                                        .inputType(ImageGenerateService.Request.class)
                                        .build()
                        );
                        break;
                    default:
                        log.warn("未知的工具名称: {}", toolName);
                        break;
                }
            } catch (Exception e) {
                log.error("构建工具回调失败: {}", toolName, e);
            }
        }

        return toolCallbacks;
    }

}
