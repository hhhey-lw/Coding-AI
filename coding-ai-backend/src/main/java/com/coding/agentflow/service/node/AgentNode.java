package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.core.manager.tool.ImageGenerateService;
import com.coding.core.manager.tool.MusicGenerateService;
import com.coding.core.model.entity.KnowledgeVectorDO;
import com.coding.core.repository.KnowledgeVectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
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
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取配置参数 - 需要支持ToolCall
        String chatModelName = getConfigParamAsString(node, "chatModel", "qwen-plus");
        String userPrompt = getConfigParamAsString(node, "prompt", "");
        // 知识库相关参数
        List<String> knowledgeBaseIds = getConfigParamAsList(node, "knowledgeBaseIds");
        Integer topK = getConfigParamAsInteger(node, "topK", 5);
        String embeddingModelName = getConfigParamAsString(node, "embeddingModel", "");
        String rerankModelName = getConfigParamAsString(node, "rerankModel", "");
        // 工具参数
        List<String> tools = getConfigParamAsList(node, "tools");

        log.info("执行Agent节点，模型: {}, 知识库: {}, 工具: {}", chatModel, knowledgeBaseIds, tools);

        // 补充系统提示词 - 1. 嵌入知识库碎片知识 2. 组合系统提示词&用户提示词
        String finalUserPrompt = replaceTemplateWithVariable(userPrompt, context);
        String finalSystemPrompt = queryKnowledgeBaseAndBuildPrompts(knowledgeBaseIds, embeddingModelName, rerankModelName, finalUserPrompt, topK);

        // 构建ChatClient - 工具调用 & 思考内容
        ChatClient.ChatClientRequestSpec chatRequest = buildChatRequest(this.chatModel, chatModelName, tools, finalSystemPrompt, finalUserPrompt);

        // Agent通常会进行多轮思考和工具调用
        String output = chatRequest.call().content();

        // 包装返回结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("reasoning", "Agent推理过程（待实现）");
        resultData.put("answer", output);

        return NodeExecutionResult.success(resultData);
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
    private ChatClient.ChatClientRequestSpec buildChatRequest(ChatModel chatModel, String chatModelName, List<String> tools, String systemPrompt, String userPrompt) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        List<ToolCallback> toolCallbacks = buildToolCallbacks(tools);

        return chatClient
                .prompt()
                .messages(List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userPrompt)
                ))
                .options(OpenAiChatOptions.builder()
                        .model(chatModelName)
                        .internalToolExecutionEnabled(true)
                        .streamUsage(true)
                        .parallelToolCalls(true)
                        .build())
                .toolCallbacks(toolCallbacks);
    }

    /**
     * 检索模型知识库并构建系统提示词
     */
    private String queryKnowledgeBaseAndBuildPrompts(List<String> knowledgeBaseIds, String embeddingModelName, String rerankModelName, String finalUserPrompt, Integer topK) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty() || StringUtils.isBlank(finalUserPrompt)) {
            return "你是一个乐于助人的AI助手，能够帮助用户完成任务。";
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

            // 3. 如果没有检索到文档，返回默认提示词
            if (allRetrievedDocs.isEmpty()) {
                log.info("未从知识库中检索到相关文档");
                return "你是一个乐于助人的AI助手，能够帮助用户完成任务。";
            }

            // 4. TODO: 如果配置了重排序模型，可以在这里进行重排序
            // 目前简单按照相似度排序（假设数据库已经按相似度排序）
            List<KnowledgeVectorDO> topDocs = allRetrievedDocs.stream()
                    .sorted(Comparator.comparing(KnowledgeVectorDO::getSimilarity).reversed())
                    .limit(topK)
                    .toList();

            // 5. 构建包含知识库内容的系统提示词
            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("你是一个乐于助人的AI助手，能够帮助用户完成任务。\n\n");
            systemPrompt.append("以下是从知识库中检索到的相关信息，请参考这些信息来回答用户的问题：\n\n");

            for (int i = 0; i < topDocs.size(); i++) {
                KnowledgeVectorDO doc = topDocs.get(i);
                systemPrompt.append("[文档 ").append(i + 1).append("]\n");
                systemPrompt.append(doc.getContent()).append("\n\n");
            }

            systemPrompt.append("请基于以上知识库内容，结合你的知识，准确、专业地回答用户的问题。 如果知识库内容不足以回答问题，可以说明无法回答。\n");

            return systemPrompt.toString();

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
