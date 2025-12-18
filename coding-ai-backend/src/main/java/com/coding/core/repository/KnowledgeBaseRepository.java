package com.coding.core.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coding.core.model.entity.KnowledgeBaseDO;
import com.coding.core.model.request.KnowledgeBasePageRequest;

/**
 * 知识库Repository
 *
 * @author weilong
 */
public interface KnowledgeBaseRepository {

    /**
     * 新增知识库
     *
     * @param knowledgeBase 知识库实体
     * @return 是否成功
     */
    Boolean save(KnowledgeBaseDO knowledgeBase);

    /**
     * 更新知识库
     *
     * @param knowledgeBase 知识库实体
     * @return 是否成功
     */
    Boolean update(KnowledgeBaseDO knowledgeBase);

    /**
     * 根据ID删除知识库（逻辑删除）
     *
     * @param id 知识库ID
     * @return 是否成功
     */
    Boolean deleteById(Long id);

    /**
     * 根据ID查询知识库
     *
     * @param id 知识库ID
     * @return 知识库实体
     */
    KnowledgeBaseDO getById(Long id);

    /**
     * 分页查询知识库
     *
     * @param userId   用户Id
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    IPage<KnowledgeBaseDO> page(Long userId, String name, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 增加知识库的向量数量
     *
     * @param id    知识库ID
     * @param count 增加的数量
     * @return 是否成功
     */
    Boolean increaseVectorCount(Long id, Long count);

    /**
     * 减少知识库的向量数量
     *
     * @param id    知识库ID
     * @param count 减少的数量
     * @return 是否成功
     */
    Boolean decreaseVectorCount(Long id, Long count);
}

