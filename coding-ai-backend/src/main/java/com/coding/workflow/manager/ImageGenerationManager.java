package com.coding.workflow.manager;

import com.coding.workflow.model.request.ImageGenerationRequest;
import com.coding.workflow.model.response.ImageGenerationResponse;
import com.coding.workflow.service.ai.ImageGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 图像生成管理器
 * 负责管理所有图像生成服务提供商并提供统一的调用入口
 */
@Slf4j
@Component
public class ImageGenerationManager {

    private final Map<String, ImageGenerationService> serviceMap;

    @Autowired
    public ImageGenerationManager(List<ImageGenerationService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                    ImageGenerationService::getProvider,
                    Function.identity()
                ));

        log.info("图像生成管理器初始化完成，已注册服务: {}", serviceMap.keySet());
    }

    /**
     * 生成图像
     *
     * @param request 图像生成请求
     * @return 图像生成响应
     */
    public ImageGenerationResponse generateImages(ImageGenerationRequest request) {
        String provider = request.getProvider();

        // 如果没有指定供应商，默认使用 volcengine
        if (StringUtils.isBlank(provider)) {
            provider = "volcengine";
            request.setProvider(provider);
            log.info("未指定图像生成供应商，使用默认供应商: {}", provider);
        }

        ImageGenerationService service = serviceMap.get(provider);
        if (service == null) {
            log.error("未找到图像生成服务提供商: {}, 可用的提供商: {}", provider, serviceMap.keySet());
            return ImageGenerationResponse.builder()
                    .success(false)
                    .errorMessage("未找到图像生成服务提供商: " + provider)
                    .errorCode("PROVIDER_NOT_FOUND")
                    .build();
        }

        // 检查模型支持
        if (!service.supportsModel(request.getModelId())) {
            log.error("供应商 {} 不支持模型: {}", provider, request.getModelId());
            return ImageGenerationResponse.builder()
                    .success(false)
                    .errorMessage("供应商 " + provider + " 不支持模型: " + request.getModelId())
                    .errorCode("MODEL_NOT_SUPPORTED")
                    .build();
        }

        log.info("使用供应商 {} 生成图像, 模型: {}", provider, request.getModelId());
        return service.generateImages(request);
    }

    /**
     * 获取所有可用的服务提供商
     *
     * @return 服务提供商列表
     */
    public List<String> getAvailableProviders() {
        return serviceMap.keySet().stream().collect(Collectors.toList());
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
