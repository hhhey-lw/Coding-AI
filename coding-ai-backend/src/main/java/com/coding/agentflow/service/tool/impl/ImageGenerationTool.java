package com.coding.agentflow.service.tool.impl;

import com.coding.agentflow.service.tool.AbstractAgentTool;
import com.coding.core.manager.tool.ImageGenerateService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("generateImage")
@RequiredArgsConstructor
public class ImageGenerationTool extends AbstractAgentTool<ImageGenerateService.Request, ImageGenerateService.Response> {

    private final ImageGenerateService imageGenerateService;

    @Override
    public String getName() {
        return "generateImage";
    }

    @Override
    public String getDescription() {
        return "根据图片提示词和参考图生成对应的图片，并返回图片的URL地址";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of(
                "prompt", "生成图片的提示词",
                "referenceImage", "参考图URL地址（可选）"
        );
    }

    @Override
    protected Class<ImageGenerateService.Request> getInputType() {
        return ImageGenerateService.Request.class;
    }

    @Override
    public ImageGenerateService.Response apply(ImageGenerateService.Request request) {
        return imageGenerateService.apply(request);
    }
}
