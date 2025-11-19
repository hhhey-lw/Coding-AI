package com.coding.core.manager.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * TODO 调整为图片生成服务
 */
@Component
public class ImageGenerateService implements Function<ImageGenerateService.Request, ImageGenerateService.Response> {
    
    @Override
    public Response apply(Request request) {
        return "http://example.com/image.png".equals(request.prompt) ?
                new Response("https://p0.qhimg.com/bdm/970_600_85/t019212816a7f3b0f83.jpg") :
                new Response("https://p0.qhimg.com/bdm/970_600_85/t019212816a7f3b0f83.jpg");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("图片生成请求")
    public record Request(@JsonProperty(required = true, value = "prompt") @JsonPropertyDescription("生成图片的提示词") String prompt,
                          @JsonProperty(required = false, value = "referenceImage") @JsonPropertyDescription("参考图URL地址") String referenceImage){}

    public record Response(String imageUrl){}
}
