package com.coding.workflow.model.request;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 音乐生成请求模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicGenerationRequest {

    /**
     * 供应商名称
     */
    private String provider;

    /**
     * 模型ID
     */
    private String model;

    /**
     * 生成提示词
     */
    private String prompt;

    /**
     * 歌词内容
     */
    private String lyrics;

    /**
     * 音频设置
     */
    private AudioSetting audioSetting;

    /**
     * 音频设置类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioSetting {
        /**
         * 采样率
         */
        private Integer sampleRate;

        /**
         * 比特率
         */
        private Integer bitrate;

        /**
         * 音频格式
         */
        private String format;

        /**
         * 输出格式
         */
        private String outputFormat;
    }
}
