package com.coding.admin.manager.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

public class ImageGenerateService implements Function<ImageGenerateService.Request, ImageGenerateService.Response> {
    
    @Override
    public Response apply(Request request) {
        return "http://example.com/image.png".equals(request.prompt) ?
                new Response("http://example.com/image.png") :
                new Response("http://example.com/default_image.png");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("图片生成请求")
    public record Request(@JsonProperty(required = true, value = "prompt") @JsonPropertyDescription("生成图片的提示词") String prompt,
                          @JsonProperty(required = true, value = "referenceImage") @JsonPropertyDescription("参考图URL地址") String referenceImage){}

    public record Response(String imageUrl){}
}
