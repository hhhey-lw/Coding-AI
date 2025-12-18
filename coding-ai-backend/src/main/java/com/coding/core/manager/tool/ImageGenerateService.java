package com.coding.core.manager.tool;

import com.coding.workflow.model.request.ImageGenerationRequest;
import com.coding.workflow.model.response.ImageGenerationResponse;
import com.coding.workflow.service.ai.ImageGenerationService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;


@Component
@AllArgsConstructor
public class ImageGenerateService implements Function<ImageGenerateService.Request, ImageGenerateService.Response> {

    private final ImageGenerationService imageGenerationService;

    @Override
    public Response apply(Request request) {
        ImageGenerationRequest.ImageGenerationRequestBuilder generationRequestBuilder = ImageGenerationRequest.builder()
                .prompt(request.prompt)
                // 每次只生成一张图片
                .maxImages(1)
                .watermark(false)
                .modelId("doubao-seedream-4-0-250828");
        if (StringUtils.isNotBlank(request.referenceImage)) {
            generationRequestBuilder.imageUrls(List.of(request.referenceImage));
        }
        if (StringUtils.isNotBlank(request.size)) {
            generationRequestBuilder.size(request.size);
        }
        ImageGenerationRequest generationRequest = generationRequestBuilder.build();

        ImageGenerationResponse generationResponse = imageGenerationService.generateImages(generationRequest);
        return new Response(generationResponse.getImageUrls().get(0));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("图片生成请求")
    public record Request(@JsonProperty(required = true, value = "prompt") @JsonPropertyDescription("生成图片的提示词") String prompt,
                          @JsonProperty(required = false, value = "referenceImage") @JsonPropertyDescription("参考图URL地址") String referenceImage,
                          @JsonProperty(required = false, value = "size") @JsonPropertyDescription("待生成图片的尺寸, 范围是1280x720 - 4096x4096，默认2028x2048") String size){}

    public record Response(String imageUrl){}
}
