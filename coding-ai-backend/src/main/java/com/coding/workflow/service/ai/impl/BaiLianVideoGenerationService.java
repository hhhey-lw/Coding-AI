package com.coding.workflow.service.ai.impl;

import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.coding.core.enums.ModelTypeEnum;
import com.coding.core.service.AiProviderConfigService;
import com.coding.workflow.model.request.VideoGenerationRequest;
import com.coding.workflow.model.response.VideoGenerationResponse;
import com.coding.workflow.service.ai.VideoGenerationService;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

/**
 * 阿里云DashScope 视频生成服务实现
 */
@Slf4j
@Service
public class BaiLianVideoGenerationService implements VideoGenerationService {

    private static final String PROVIDER_NAME = "BaiLian";

    private String apiKey;

    @Resource
    private AiProviderConfigService aiProviderConfigService;

    @PostConstruct
    public void init() {
        this.apiKey = aiProviderConfigService.getByProviderCodeAndServiceType(PROVIDER_NAME, ModelTypeEnum.VideoGen.name()).getAuthorization();
        log.info("BaiLian视频生成服务初始化完成");
    }

    @Override
    public VideoGenerationResponse generateVideo(VideoGenerationRequest request) {
        try {
            log.info("开始使用BaiLian生成视频, model: {}, prompt: {}", request.getModel(), request.getPrompt());

            // 调用BaiLian API
            VideoSynthesisResult result = callBaiLianVideoGen(request);

            // 处理响应
            return processResponse(result);

        } catch (Exception e) {
            log.error("BaiLian视频生成失败", e);
            return VideoGenerationResponse.builder()
                    .success(false)
                    .errorMessage("BaiLian视频生成失败: " + e.getMessage())
                    .errorCode("BaiLian_ERROR")
                    .build();
        }
    }

    /**
     * 调用BaiLian视频生成API
     */
    private VideoSynthesisResult callBaiLianVideoGen(VideoGenerationRequest request)
            throws NoApiKeyException, InputRequiredException {

        VideoGenerationRequest.VideoSetting setting = request.getVideoSetting();

        // 构建参数
        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("prompt_extend", setting != null && setting.getPromptExtend() != null ?
            setting.getPromptExtend() : true);
        parameters.put("resolution", setting != null && StringUtils.isNotBlank(setting.getResolution()) ?
            setting.getResolution() : "720P");
        parameters.put("duration", setting != null && setting.getDuration() != null ?
            setting.getDuration() : 5);
        parameters.put("watermark", setting != null && setting.getWatermark() != null ?
            setting.getWatermark() : false);

        // 构建请求参数
        VideoSynthesisParam.VideoSynthesisParamBuilder builder = VideoSynthesisParam.builder()
                .apiKey(apiKey)
                .model(request.getModel())
                .prompt(request.getPrompt())
                .parameters(parameters);

        // 添加首帧图像
        if (setting != null && StringUtils.isNotBlank(setting.getFirstFrameImage())) {
            builder.firstFrameUrl(setting.getFirstFrameImage());
        }

        // 添加尾帧图像
        if (setting != null && StringUtils.isNotBlank(setting.getLastFrameImage())) {
            builder.lastFrameUrl(setting.getLastFrameImage());
        }

        VideoSynthesisParam param = builder.build();
        VideoSynthesis videoSynthesis = new VideoSynthesis();
        return videoSynthesis.call(param);
    }

    /**
     * 处理BaiLian响应
     */
    private VideoGenerationResponse processResponse(VideoSynthesisResult result) {
        if (result != null && result.getOutput() != null) {
            String taskStatus = result.getOutput().getTaskStatus();

            if ("SUCCEEDED".equals(taskStatus)) {
                return VideoGenerationResponse.builder()
                        .success(true)
                        .videoUrl(result.getOutput().getVideoUrl())
                        .taskStatus(taskStatus)
                        .providerSpecificData(result)
                        .build();
            } else {
                String errorMessage = result.getOutput().getMessage() != null ?
                    result.getOutput().getMessage() : "视频生成失败，状态: " + taskStatus;

                return VideoGenerationResponse.builder()
                        .success(false)
                        .taskStatus(taskStatus)
                        .errorMessage(errorMessage)
                        .errorCode("BaiLian_TASK_FAILED")
                        .providerSpecificData(result)
                        .build();
            }
        } else {
            return VideoGenerationResponse.builder()
                    .success(false)
                    .errorMessage("视频生成失败，响应为空")
                    .errorCode("BaiLian_EMPTY_RESPONSE")
                    .providerSpecificData(result)
                    .build();
        }
    }

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supportsModel(String model) {
        return StringUtils.isNotBlank(model);
    }
}
