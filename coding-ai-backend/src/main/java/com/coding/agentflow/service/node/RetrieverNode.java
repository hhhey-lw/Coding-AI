package com.coding.agentflow.service.node;

import com.coding.agentflow.model.enums.NodeTypeEnum;
import com.coding.agentflow.model.model.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

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

    @Override
    protected NodeExecutionResult doExecute(Node node, Map<String, Object> context) {
        // 获取配置参数
        String query = getConfigParamAsString(node, "query", "");
        Integer topK = (Integer) getConfigParam(node, "topK");
        List<String> knowledgeBaseIds = getConfigParamAsList(node, "knowledgeBaseId");
        // 嵌入&重排序模型
        String embeddingModel = getConfigParamAsString(node, "embeddingModel", "");
        String rerankModel = getConfigParamAsString(node, "rerankModel", "");

        log.info("执行检索节点，查询: {}, TopK: {}", query, topK);

        // TODO: 实际调用检索服务
        List<List<Map<String, Object>>> retrievedDocs = performRetrieval(query, topK, knowledgeBaseIds);

        // TODO: 调用实际的重排服务

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("query", query);
        resultData.put("topK", topK);
        resultData.put("knowledgeBaseIds", knowledgeBaseIds);
        resultData.put("documents", retrievedDocs);
        resultData.put("documentCount", retrievedDocs.stream().mapToInt(List::size).sum());

        return NodeExecutionResult.success(resultData);
    }

    /**
     * 执行检索
     */
    private List<List<Map<String, Object>>> performRetrieval(String query, Integer topK, List<String> knowledgeBaseIds) {
        // TODO: 实现实际的检索逻辑
        // 支持向量检索、关键词检索、混合检索等
        List<Map<String, Object>> results = new ArrayList<>();
        
        // 示例返回
        for (int i = 0; i < (topK != null ? topK : 3); i++) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", "doc_" + i);
            doc.put("content", "检索到的文档内容 " + i + "（待实现）");
            doc.put("score", 0.9 - i * 0.1);
            doc.put("metadata", Map.of("knowledgeBaseId", "knowledge_base_id"));
            results.add(doc);
        }
        
        return Collections.singletonList(results);
    }

    @Override
    protected boolean doValidate(Node node) {
        // 验证必需的配置参数
        String query = getConfigParamAsString(node, "query", null);
        String embeddingModel = getConfigParamAsString(node, "embeddingModel", null);
        String rerankModel = getConfigParamAsString(node, "rerankModel", null);
        if (null == query || query.isEmpty()) {
            log.error("检索节点缺少必需的query配置");
            return false;
        }
        if (null == embeddingModel || embeddingModel.isEmpty()) {
            log.error("检索节点缺少必需的embeddingModel配置");
            return false;
        }
        return true;
    }

    @Override
    public String getNodeType() {
        return NodeTypeEnum.RETRIEVER.name();
    }
}
