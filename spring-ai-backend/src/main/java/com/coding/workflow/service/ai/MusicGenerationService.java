package com.coding.workflow.service.ai;

import com.coding.workflow.model.request.MusicGenerationRequest;
import com.coding.workflow.model.response.MusicGenerationResponse;

/**
 * 音乐生成服务接口
 */
public interface MusicGenerationService {

    /**
     * 生成音乐
     *
     * @param request 音乐生成请求
     * @return 音乐生成响应
     */
    MusicGenerationResponse generateMusic(MusicGenerationRequest request);

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
