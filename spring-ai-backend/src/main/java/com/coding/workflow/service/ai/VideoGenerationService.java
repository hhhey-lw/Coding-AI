package com.coding.workflow.service.ai;

import com.coding.workflow.model.request.VideoGenerationRequest;
import com.coding.workflow.model.response.VideoGenerationResponse;

/**
 * 视频生成服务接口
 */
public interface VideoGenerationService {

    /**
     * 生成视频
     *
     * @param request 视频生成请求
     * @return 视频生成响应
     */
    VideoGenerationResponse generateVideo(VideoGenerationRequest request);

    /**
     * 获取供应商名称
     *
     * @return 供应商名称
     */
    String getProvider();

    /**
     * 是否支持该模型
     *
     * @param model 模型名称
     * @return 是否支持
     */
    boolean supportsModel(String model);
}
