package com.coding.core.manager.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 调整为音乐生成服务
 */
@Component
public class MusicGenerateService implements Function<MusicGenerateService.Request, MusicGenerateService.Response> {

    @Override
    public Response apply(Request request) {
        return "http://example.com/music.mp3".equals(request.prompt) ?
                new Response("https://cdn.hailuoai.com/prod/2025-09-25-22/moss-audio/user_music/1758812347339664609-316425092063413.mp3") :
                new Response("https://cdn.hailuoai.com/prod/2025-09-25-22/moss-audio/user_music/1758812347339664609-316425092063413.mp3");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("音乐生成请求")
    public record Request(@JsonProperty(required = true, value = "prompt") @JsonPropertyDescription("描述音乐的风格") String prompt,
                          @JsonProperty(required = true, value = "lyrics") @JsonPropertyDescription("歌词内容") String lyrics){}

    public record Response(String musicUrl){}
}
