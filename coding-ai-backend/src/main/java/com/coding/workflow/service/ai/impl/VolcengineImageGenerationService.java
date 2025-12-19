package com.coding.workflow.service.ai.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.coding.core.enums.ModelTypeEnum;
import com.coding.core.service.AiProviderConfigService;
import com.coding.workflow.model.request.ImageGenerationRequest;
import com.coding.workflow.model.response.ImageGenerationResponse;
import com.coding.workflow.service.ai.ImageGenerationService;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.model.images.generation.ResponseFormat;
import com.volcengine.ark.runtime.service.ArkService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Volcengine 图像生成服务实现
 */
@Slf4j
@Service
@Primary
public class VolcengineImageGenerationService implements ImageGenerationService {

    private static final String PROVIDER_NAME = "Volcengine";
    private static final Integer DEFAULT_MAX_IMAGES = 3;

    private ArkService arkService;

    @Resource
    private AiProviderConfigService aiProviderConfigService;

    @PostConstruct
    protected void init() {
        Dispatcher dispatcher = new Dispatcher();
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        String apiKey = aiProviderConfigService.getByProviderCodeAndServiceType(PROVIDER_NAME, ModelTypeEnum.ImageGen.name()).getAuthorization();
        this.arkService = ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .apiKey(apiKey)
                .build();
        log.info("Volcengine图像生成服务初始化完成");
    }

    @Override
    public ImageGenerationResponse generateImages(ImageGenerationRequest request) {
        try {
            log.info("开始使用Volcengine生成图像, modelId: {}, prompt: {}", request.getModelId(), request.getPrompt());

            // 构建Volcengine请求
            GenerateImagesRequest volcengineRequest = buildVolcengineRequest(request);

            // 调用Volcengine API
            ImagesResponse imagesResponse = arkService.generateImages(volcengineRequest);

            // 处理响应
            return processResponse(imagesResponse);

        } catch (Exception e) {
            log.error("Volcengine图像生成失败", e);
            return ImageGenerationResponse.builder()
                    .success(false)
                    .errorMessage("Volcengine图像生成失败: " + e.getMessage())
                    .errorCode("VOLCENGINE_ERROR")
                    .build();
        }
    }

    /**
     * 构建Volcengine请求
     */
    private GenerateImagesRequest buildVolcengineRequest(ImageGenerationRequest request) {
        // 设置顺序图像生成选项
        GenerateImagesRequest.SequentialImageGenerationOptions sequentialOptions =
                new GenerateImagesRequest.SequentialImageGenerationOptions();
        sequentialOptions.setMaxImages(request.getMaxImages() != null ? request.getMaxImages() : DEFAULT_MAX_IMAGES);

        // 创建请求对象
        GenerateImagesRequest volcengineRequest = new GenerateImagesRequest();
        volcengineRequest.setModel(request.getModelId());
        volcengineRequest.setPrompt(request.getPrompt());
        volcengineRequest.setResponseFormat(ResponseFormat.Url);
        volcengineRequest.setSize(request.getSize());
        volcengineRequest.setSequentialImageGeneration(request.getMaxImages() == 1 ? "disabled" : "auto");
        volcengineRequest.setSequentialImageGenerationOptions(sequentialOptions);
        volcengineRequest.setStream(false);
        volcengineRequest.setWatermark(request.getWatermark() != null ? request.getWatermark() : false);

        // 添加参考图像
        if (CollectionUtil.isNotEmpty(request.getImageUrls())) {
            volcengineRequest.setImage(request.getImageUrls());
        }

        return volcengineRequest;
    }

    /**
     * 处理Volcengine响应
     */
    private ImageGenerationResponse processResponse(ImagesResponse imagesResponse) {
        if (imagesResponse != null && imagesResponse.getData() != null) {
            List<String> imageUrls = imagesResponse.getData().stream()
                    .map(ImagesResponse.Image::getUrl)
                    .collect(Collectors.toList());

            return ImageGenerationResponse.builder()
                    .success(true)
                    .imageUrls(imageUrls)
                    .build();
        } else {
            String errorMessage = Objects.nonNull(imagesResponse) && Objects.nonNull(imagesResponse.getError())
                    ? imagesResponse.getError().toString()
                    : "图片生成失败";

            return ImageGenerationResponse.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .errorCode("VOLCENGINE_API_ERROR")
                    .build();
        }
    }

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supportsModel(String modelId) {
        return StringUtils.isNotBlank(modelId);
    }
}
