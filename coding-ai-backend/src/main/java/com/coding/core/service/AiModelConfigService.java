package com.coding.core.service;

import com.coding.core.model.vo.AiModelConfigVO;

import java.util.List;

/**
 * AI模型服务接口
 */
public interface AiModelConfigService {

    /**
     * 根据模型类型获取启用的AI模型列表
     */
    List<AiModelConfigVO> getEnabledModelsByType(String modelType);
}
