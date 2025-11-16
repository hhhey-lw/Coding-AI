package com.coding.core.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coding.core.model.entity.KnowledgeVectorDO;
import com.coding.core.model.request.KnowledgeVectorPageRequest;

import java.util.List;

/**
 * 知识向量Repository
 * @author weilong
 */
public interface KnowledgeVectorRepository {

    /**
     * 新增向量
     * @param vector 向量实体
     * @return 是否成功
     */
    Boolean save(KnowledgeVectorDO vector);

    /**
     * 批量新增向量
     * @param vectors 向量列表
     * @return 是否成功
     */
    Boolean saveBatch(List<KnowledgeVectorDO> vectors);

    /**
     * 更新向量
     * @param vector 向量实体
     * @return 是否成功
     */
    Boolean update(KnowledgeVectorDO vector);

    /**
     * 根据ID删除向量（逻辑删除）
     * @param id 向量ID
     * @return 是否成功
     */
    Boolean deleteById(String id);

    /**
     * 根据知识库ID删除所有向量（逻辑删除）
     * @param knowledgeBaseId 知识库ID
     * @return 是否成功
     */
    Boolean deleteByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 根据ID查询向量
     * @param id 向量ID
     * @return 向量实体
     */
    KnowledgeVectorDO getById(String id);

    /**
     * 分页查询向量
     * @param request 分页请求
     * @return 分页结果
     */
    IPage<KnowledgeVectorDO> page(KnowledgeVectorPageRequest request);

    /**
     * 根据知识库ID统计向量数量
     * @param knowledgeBaseId 知识库ID
     * @return 向量数量
     */
    Long countByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 相似性搜索
     * @param knowledgeBaseId 知识库ID
     * @param embedding 查询向量
     * @param topK 返回数量
     * @return 相似向量列表
     */
    List<KnowledgeVectorDO> similaritySearch(Long knowledgeBaseId, String embedding, Integer topK);
}

