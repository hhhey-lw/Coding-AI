package com.coding.core.manager.tool;

import com.coding.workflow.model.request.MusicGenerationRequest;
import com.coding.workflow.model.response.MusicGenerationResponse;
import com.coding.workflow.service.ai.MusicGenerationService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 调整为音乐生成服务
 */
@Component
@AllArgsConstructor
public class MusicGenerateService implements Function<MusicGenerateService.Request, MusicGenerateService.Response> {

    private final MusicGenerationService musicGenerationService;

    @Override
    public Response apply(Request request) {
        MusicGenerationRequest musicGenerationRequest = MusicGenerationRequest.builder()
                .prompt(request.prompt)
                .lyrics(request.lyrics)
                .model("music-1.5")
                .build();

        MusicGenerationResponse musicGenerationResponse = musicGenerationService.generateMusic(musicGenerationRequest);

        return new Response(musicGenerationResponse.getAudioUrl());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("音乐生成请求")
    public record Request(@JsonProperty(required = true, value = "prompt") @JsonPropertyDescription("描述音乐的风格") String prompt,
                          @JsonProperty(required = true, value = "lyrics") @JsonPropertyDescription("歌词内容") String lyrics){}

    public record Response(String musicUrl){}
}
