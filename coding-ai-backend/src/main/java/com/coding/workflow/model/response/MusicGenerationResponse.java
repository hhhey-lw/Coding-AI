package com.coding.workflow.model.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 音乐生成响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicGenerationResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 生成的音频URL
     */
    private String audioUrl;

    /**
     * 音乐时长（毫秒）
     */
    private Long duration;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 供应商特定的响应数据
     */
    private Object providerSpecificData;
}
