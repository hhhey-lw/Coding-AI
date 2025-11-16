package com.coding.workflow.manager;

import com.coding.workflow.model.request.VideoGenerationRequest;
import com.coding.workflow.model.response.VideoGenerationResponse;
import com.coding.workflow.service.ai.VideoGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视频生成管理器
 * 负责管理所有视频生成服务提供商并提供统一的调用入口
 */
@Slf4j
@Component
public class VideoGenerationManager {

    private final Map<String, VideoGenerationService> serviceMap;

    @Autowired
    public VideoGenerationManager(List<VideoGenerationService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                    VideoGenerationService::getProvider,
                    Function.identity()
                ));

        log.info("视频生成管理器初始化完成，已注册服务: {}", serviceMap.keySet());
    }

    /**
     * 生成视频
     *
     * @param request 视频生成请求
     * @return 视频生成响应
     */
    public VideoGenerationResponse generateVideo(VideoGenerationRequest request) {
        String provider = request.getProvider();

        // 如果没有指定供应商，默认使用BaiLian
        if (StringUtils.isBlank(provider)) {
            provider = "BaiLian";
            request.setProvider(provider);
            log.info("未指定视频生成供应商，使用默认供应商: {}", provider);
        }

        VideoGenerationService service = serviceMap.get(provider);
        if (service == null) {
            log.error("未找到视频生成服务提供商: {}, 可用的提供商: {}", provider, serviceMap.keySet());
            return VideoGenerationResponse.builder()
                    .success(false)
                    .errorMessage("未找到视频生成服务提供商: " + provider)
                    .errorCode("PROVIDER_NOT_FOUND")
                    .build();
        }

        // 检查模型支持
        if (!service.supportsModel(request.getModel())) {
            log.error("供应商 {} 不支持模型: {}", provider, request.getModel());
            return VideoGenerationResponse.builder()
                    .success(false)
                    .errorMessage("供应商 " + provider + " 不支持模型: " + request.getModel())
                    .errorCode("MODEL_NOT_SUPPORTED")
                    .build();
        }

        log.info("使用供应商 {} 生成视频, 模型: {}", provider, request.getModel());
        return service.generateVideo(request);
    }

    /**
     * 获取所有可用的服务提供商
     *
     * @return 服务提供商列表
     */
    public List<String> getAvailableProviders() {
        return new ArrayList<>(serviceMap.keySet());
    }

    /**
     * 检查指定供应商是否可用
     *
     * @param provider 供应商名称
     * @return 是否可用
     */
    public boolean isProviderAvailable(String provider) {
        return serviceMap.containsKey(provider);
    }
}
