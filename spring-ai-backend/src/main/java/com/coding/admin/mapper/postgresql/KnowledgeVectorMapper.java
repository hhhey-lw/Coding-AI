package com.coding.admin.mapper.postgresql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.admin.model.entity.KnowledgeVectorDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识向量Mapper
 * @author weilong
 */
@Mapper
public interface KnowledgeVectorMapper extends BaseMapper<KnowledgeVectorDO> {

    /**
     * 根据知识库ID统计向量数量
     * @param knowledgeBaseId 知识库ID
     * @return 向量数量
     */
    Long countByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
     * 相似性搜索
     * @param knowledgeBaseId 知识库ID
     * @param embedding 查询向量
     * @param topK 返回数量
     * @return 相似向量列表
     */
    List<KnowledgeVectorDO> similaritySearch(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("embedding") String embedding,
            @Param("topK") Integer topK
    );
}

