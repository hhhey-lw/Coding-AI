package com.coding.admin.mapper.postgresql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.admin.model.entity.KnowledgeBaseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识库Mapper
 * @author weilong
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseDO> {

    /**
     * 增加知识库的向量数量
     * @param id 知识库ID
     * @param count 增加的数量
     * @return 更新的行数
     */
    int increaseVectorCount(@Param("id") Long id, @Param("count") Long count);

    /**
     * 减少知识库的向量数量
     * @param id 知识库ID
     * @param count 减少的数量
     * @return 更新的行数
     */
    int decreaseVectorCount(@Param("id") Long id, @Param("count") Long count);
}

