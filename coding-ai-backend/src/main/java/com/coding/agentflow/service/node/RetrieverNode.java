package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import com.coding.graph.core.state.OverAllState;
import com.coding.core.model.entity.KnowledgeVectorDO;
import com.coding.core.repository.KnowledgeVectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 检索节点
 * 从向量数据库或知识库中检索相关信息
 * 1. 支持多个PGVector数据库
 * 2. 仅支持嵌入模型
 * 3. 重排序(当存在多个向量数据库时)
 */
@Slf4j
@Component
public class RetrieverNode extends AbstractNode {

    private final Integer DEFAULT_DIMENSIONS;
    private final EmbeddingModel embeddingModel;
    private final KnowledgeVectorRepository knowledgeVectorRepository;

    public RetrieverNode(
            @Value("${spring.ai.openai.embedding.options.dimensions:1536}") Integer defaultDimensions,
            EmbeddingModel embeddingModel,
            KnowledgeVectorRepository knowledgeVectorRepository) {
        this.DEFAULT_DIMENSIONS = defaultDimensions;
        this.embeddingModel = embeddingModel;
        this.knowledgeVectorRepository = knowledgeVectorRepository;
    }

    @Override
    protected Map<String, Object> doExecute(Node node, OverAllState state) {
        // 获取配置参数
        String query = getConfigParamAsString(node, "query");
        Integer topK = getConfigParamAsInteger(node, "topK", 5);
        List<String> knowledgeBaseIds = getConfigParamAsList(node, "knowledgeBaseId");
        String embeddingModelName = getConfigParamAsString(node, "embeddingModel");
        String rerankModelName = getConfigParamAsString(node, "rerankModel");

        // 支持从上下文中替换查询变量
        String finalQuery = replaceTemplateWithVariable(query, state);

        log.info("执行检索节点，查询: {}, TopK: {}, 知识库数量: {}", finalQuery, topK, 
                knowledgeBaseIds != null ? knowledgeBaseIds.size() : 0);

        // 执行检索
        List<Map<String, Object>> retrievedDocs = performRetrieval(finalQuery, topK, knowledgeBaseIds, embeddingModelName);

        // TODO: 如果配置了重排序模型，可以在这里进行重排序
        if (StringUtils.isNotBlank(rerankModelName)) {
            log.info("重排序模型已配置: {}，但暂未实现", rerankModelName);
            // retrievedDocs = performRerank(retrievedDocs, finalQuery, rerankModelName, topK);
        }

        // 构建结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("query", finalQuery);
        resultData.put("topK", topK);
        resultData.put("knowledgeBaseIds", knowledgeBaseIds);
        resultData.put("documents", retrievedDocs);
        resultData.put("documentCount", retrievedDocs.size());

        return resultData;
    }

    /**
     * 执行检索 - 从多个知识库中检索相关文档
     * 
     * @param query 查询文本
     * @param topK 每个知识库返回的文档数量
     * @param knowledgeBaseIds 知识库ID列表
     * @param embeddingModelName 嵌入模型名称
     * @return 检索到的文档列表
     */
    private List<Map<String, Object>> performRetrieval(String query, Integer topK, 
                                                        List<String> knowledgeBaseIds, 
                                                        String embeddingModelName) {
        if (StringUtils.isBlank(query)) {
            log.warn("查询文本为空，返回空结果");
            return Collections.emptyList();
        }

        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            log.warn("知识库ID列表为空，返回空结果");
            return Collections.emptyList();
        }

        try {
            // 1. 使用嵌入模型将查询转换为向量
            EmbeddingRequest embeddingRequest = new EmbeddingRequest(
                    List.of(query), 
                    OpenAiEmbeddingOptions.builder()
                            .model(embeddingModelName)
                            .dimensions(DEFAULT_DIMENSIONS)
                            .build()
            );
            
            EmbeddingResponse embeddingResponse = embeddingModel.call(embeddingRequest);
            if (embeddingResponse == null || embeddingResponse.getResults().isEmpty()) {
                log.warn("嵌入模型返回空结果");
                return Collections.emptyList();
            }

            // 2. 获取查询向量并转换为字符串格式
            float[] queryEmbedding = embeddingResponse.getResult().getOutput();
            String embeddingStr = convertEmbeddingToString(queryEmbedding);

            // 3. 从每个知识库中检索相关文档
            List<KnowledgeVectorDO> allRetrievedDocs = new ArrayList<>();
            for (String knowledgeBaseIdStr : knowledgeBaseIds) {
                try {
                    Long knowledgeBaseId = Long.parseLong(knowledgeBaseIdStr);
                    List<KnowledgeVectorDO> docs = knowledgeVectorRepository.similaritySearch(
                            knowledgeBaseId, embeddingStr, topK);
                    
                    if (docs != null && !docs.isEmpty()) {
                        log.info("从知识库 {} 检索到 {} 个文档", knowledgeBaseId, docs.size());
                        allRetrievedDocs.addAll(docs);
                    }
                } catch (NumberFormatException e) {
                    log.error("知识库ID格式错误: {}", knowledgeBaseIdStr, e);
                }
            }

            // 4. 如果没有检索到文档
            if (allRetrievedDocs.isEmpty()) {
                log.info("未从知识库中检索到相关文档");
                return Collections.emptyList();
            }

            // 5. 按相似度排序并限制返回数量
            List<KnowledgeVectorDO> topDocs = allRetrievedDocs.stream()
                    .sorted(Comparator.comparing(KnowledgeVectorDO::getSimilarity).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());

            // 6. 转换为Map格式返回
            return topDocs.stream()
                    .map(this::convertToDocumentMap)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("检索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 将嵌入向量转换为字符串格式
     */
    private String convertEmbeddingToString(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) builder.append(",");
            builder.append(embedding[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 将KnowledgeVectorDO转换为Map格式
     */
    private Map<String, Object> convertToDocumentMap(KnowledgeVectorDO doc) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", doc.getId());
        result.put("content", doc.getContent());
        result.put("similarity", doc.getSimilarity());
        result.put("knowledgeBaseId", doc.getKnowledgeBaseId());
        result.put("fileName", doc.getFileName());
        result.put("fileType", doc.getFileType());
        result.put("metadata", doc.getMetadata());
        return result;
    }

    /**
     * 构建检索内容的文本格式，供后续节点使用
     */
    private String buildRetrievedContent(List<Map<String, Object>> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Map<String, Object> doc = documents.get(i);
            content.append("[文档 ").append(i + 1).append("]\n");
            content.append(doc.get("content")).append("\n\n");
        }
        return content.toString();
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String query = getConfigParamAsString(node, "query");
        if (StringUtils.isBlank(query)) {
            log.error("检索节点缺少必需的query配置");
            return false;
        }

        String embeddingModel = getConfigParamAsString(node, "embeddingModel");
        if (StringUtils.isBlank(embeddingModel)) {
            log.error("检索节点缺少必需的embeddingModel配置");
            return false;
        }

        List<String> knowledgeBaseIds = getConfigParamAsList(node, "knowledgeBaseId");
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            log.error("检索节点缺少必需的knowledgeBaseId配置");
            return false;
        }

        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.RETRIEVER.name();
    }
}
