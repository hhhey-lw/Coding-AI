package com.coding.workflow.agent;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

public class MusicGenerateService implements Function<MusicGenerateService.Request, MusicGenerateService.Response> {

    @Override
    public Response apply(Request request) {
        return "http://example.com/music.mp3".equals(request.prompt) ?
                new Response("http://example.com/music.mp3") :
                new Response("http://example.com/default_music.mp3");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("音乐生成请求")
    public record Request(@JsonProperty(required = true, value = "prompt") @JsonPropertyDescription("描述音乐的风格") String prompt,
                          @JsonProperty(required = true, value = "lyrics") @JsonPropertyDescription("歌词内容") String lyrics){}

    public record Response(String musicUrl){}
}
