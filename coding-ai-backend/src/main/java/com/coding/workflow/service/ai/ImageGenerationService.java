package com.coding.workflow.service.ai;

import com.coding.workflow.model.request.ImageGenerationRequest;
import com.coding.workflow.model.response.ImageGenerationResponse;

/**
 * 图像生成服务接口
 */
public interface ImageGenerationService {

    /**
     * 生成图像
     *
     * @param request 图像生成请求
     * @return 图像生成响应
     */
    ImageGenerationResponse generateImages(ImageGenerationRequest request);

    /**
     * 获取供应商名称
     *
     * @return 供应商名称
     */
    String getProvider();

    /**
     * 是否支持该模型
     *
     * @param modelId 模型ID
     * @return 是否支持
     */
    boolean supportsModel(String modelId);
}
