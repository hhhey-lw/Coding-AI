package com.coding.admin.service;

import com.coding.admin.model.vo.AiModelBaseVO;

import java.util.List;

/**
 * AI模型服务接口
 */
public interface AiModelService {

    /**
     * 根据模型类型获取启用的AI模型列表
     */
    List<AiModelBaseVO> getEnabledModelsByType(String modelType);
}
