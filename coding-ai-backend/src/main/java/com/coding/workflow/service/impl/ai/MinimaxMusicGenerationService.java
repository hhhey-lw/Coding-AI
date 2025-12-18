package com.coding.workflow.service.impl.ai;

import com.coding.core.enums.ModelTypeEnum;
import com.coding.core.service.AiProviderConfigService;
import com.coding.workflow.model.request.MusicGenerationRequest;
import com.coding.workflow.model.response.MusicGenerationResponse;
import com.coding.workflow.service.ai.MusicGenerationService;
import com.coding.workflow.utils.AssertUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimax 音乐生成服务实现
 */
@Primary
@Slf4j
@Service
public class MinimaxMusicGenerationService implements MusicGenerationService {

    private static final String PROVIDER_NAME = "MiniMax";

    private String apiKey;
    private String url;

    private RestTemplate restTemplate;

    @Resource
    private AiProviderConfigService aiProviderConfigService;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        this.apiKey = aiProviderConfigService.getByProviderCodeAndServiceType(PROVIDER_NAME, ModelTypeEnum.MusicGen.name()).getAuthorization();
        this.url = aiProviderConfigService.getByProviderCodeAndServiceType(PROVIDER_NAME, ModelTypeEnum.MusicGen.name()).getBaseUrl();
        log.info("Minimax音乐生成服务初始化完成");
    }

    @Override
    public MusicGenerationResponse generateMusic(MusicGenerationRequest request) {
        try {
            log.info("开始使用Minimax生成音乐, model: {}, prompt: {}", request.getModel(), request.getPrompt());
            // 提示词长度：10 - 2000
            if (StringUtils.isNotBlank(request.getPrompt()) && (1000 < request.getPrompt().length() || request.getPrompt().length() < 10)) {
                throw new IllegalAccessException("提示词长度不合法");
            }

            // 调用Minimax API
            MinimaxMusicGenResponse response = callMinimaxMusicGen(request);

            // 处理响应
            return processResponse(response);

        } catch (Exception e) {
            log.error("Minimax音乐生成失败", e);
            return MusicGenerationResponse.builder()
                    .success(false)
                    .errorMessage("Minimax音乐生成失败: " + e.getMessage())
                    .errorCode("MINIMAX_ERROR")
                    .build();
        }
    }

    /**
     * 调用Minimax音乐生成API
     */
    private MinimaxMusicGenResponse callMinimaxMusicGen(MusicGenerationRequest request) {
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 构建请求体
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", request.getModel());
        payload.put("prompt", request.getPrompt());
        payload.put("lyrics", request.getLyrics());

        // 设置音频配置
        Map<String, Object> audioSetting = new HashMap<>();
        if (request.getAudioSetting() != null) {
            MusicGenerationRequest.AudioSetting setting = request.getAudioSetting();
            audioSetting.put("sample_rate", setting.getSampleRate() != null ? setting.getSampleRate() : 24000);
            audioSetting.put("bitrate", setting.getBitrate() != null ? setting.getBitrate() : 128000);
            audioSetting.put("format", StringUtils.isNotBlank(setting.getFormat()) ? setting.getFormat() : "mp3");
            payload.put("output_format", StringUtils.isNotBlank(request.getAudioSetting().getOutputFormat()) ?
                    request.getAudioSetting().getOutputFormat() : "url");
        } else {
            audioSetting.put("sample_rate", 24000);
            audioSetting.put("bitrate", 128000);
            audioSetting.put("format", "mp3");
            audioSetting.put("output_format", "url");
        }
        payload.put("audio_setting", audioSetting);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        // 发送请求
        ResponseEntity<MinimaxMusicGenResponse> response = restTemplate.postForEntity(
                url, requestEntity, MinimaxMusicGenResponse.class);

        return response.getBody();
    }

    /**
     * 处理Minimax响应
     */
    private MusicGenerationResponse processResponse(MinimaxMusicGenResponse response) {
        if (isSuccess(response)) {
            return MusicGenerationResponse.builder()
                    .success(true)
                    .audioUrl(response.getData().getAudio())
                    .duration(response.getExtraInfo() != null ? response.getExtraInfo().getMusicDuration() : null)
                    .providerSpecificData(response)
                    .build();
        } else {
            String errorMessage = response != null && response.getBaseResp() != null
                    ? response.getBaseResp().getStatusMsg()
                    : "音乐生成失败";

            return MusicGenerationResponse.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .errorCode("MINIMAX_API_ERROR")
                    .providerSpecificData(response)
                    .build();
        }
    }

    /**
     * 判断响应是否成功
     */
    private boolean isSuccess(MinimaxMusicGenResponse response) {
        return response != null && response.getBaseResp() != null
                && response.getBaseResp().getStatusCode() != null
                && response.getBaseResp().getStatusCode() == 0;
    }

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supportsModel(String model) {
        return StringUtils.isNotBlank(model);
    }

    /**
     * Minimax响应对象定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinimaxMusicGenResponse {
        private MusicData data;
        @JsonProperty("trace_id")
        private String traceId;
        @JsonProperty("extra_info")
        private ExtraInfo extraInfo;
        @JsonProperty("analysis_info")
        private Object analysisInfo;
        @JsonProperty("base_resp")
        private BaseResp baseResp;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MusicData {
            private String audio;
            private Integer status;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ExtraInfo {
            @JsonProperty("music_duration")
            private Long musicDuration;
            @JsonProperty("music_sample_rate")
            private Integer musicSampleRate;
            @JsonProperty("music_channel")
            private Integer musicChannel;
            private Integer bitrate;
            @JsonProperty("music_size")
            private Long musicSize;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BaseResp {
            @JsonProperty("status_code")
            private Integer statusCode;
            @JsonProperty("status_msg")
            private String statusMsg;
        }
    }
}
