package com.coding.workflow.model.request;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 视频生成请求模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerationRequest {

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
     * 视频设置
     */
    private VideoSetting videoSetting;

    /**
     * 供应商特定的扩展参数
     */
    private Map<String, Object> providerSpecificParams;

    /**
     * 视频设置类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoSetting {
        /**
         * 分辨率
         */
        private String resolution;

        /**
         * 时长（秒）
         */
        private Integer duration;

        /**
         * 首帧图像URL
         */
        private String firstFrameImage;

        /**
         * 尾帧图像URL
         */
        private String lastFrameImage;

        /**
         * 是否启用水印
         */
        private Boolean watermark;

        /**
         * 是否启用提示词扩展
         */
        private Boolean promptExtend;
    }
}
