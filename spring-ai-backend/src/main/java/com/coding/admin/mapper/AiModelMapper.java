package com.coding.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.admin.model.entity.AiModelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI模型Mapper
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModelDO> {

    /**
     * 根据模型类型查询启用的AI模型
     */
    List<AiModelDO> selectEnabledModelsByType(@Param("modelType") String modelType);
}
