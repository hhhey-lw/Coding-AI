package com.coding.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.core.model.entity.AiModelConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI模型Mapper
 */
@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfigDO> {

    /**
     * 根据模型类型查询启用的AI模型
     */
    List<AiModelConfigDO> selectEnabledModelsByType(@Param("modelType") String modelType);
}
