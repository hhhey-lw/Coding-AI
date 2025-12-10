package com.coding.agentflow.service.tool.impl;

import com.coding.agentflow.service.tool.AbstractAgentTool;
import com.coding.core.manager.tool.MusicGenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("generateMusic")
@RequiredArgsConstructor
public class MusicGenerationTool extends AbstractAgentTool<MusicGenerateService.Request, MusicGenerateService.Response> {

    private final MusicGenerateService musicGenerateService;

    @Override
    public String getName() {
        return "generateMusic";
    }

    @Override
    public String getDescription() {
        return "根据风格提示词和歌词内容，生成一段音乐，并返回音乐的URL地址";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of(
                "prompt", "生成音乐的风格提示词",
                "lyrics", "歌词内容"
        );
    }

    @Override
    protected Class<MusicGenerateService.Request> getInputType() {
        return MusicGenerateService.Request.class;
    }

    @Override
    public MusicGenerateService.Response apply(MusicGenerateService.Request request) {
        return musicGenerateService.apply(request);
    }
}
