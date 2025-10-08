package com.coding.workflow.model.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 视频生成响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerationResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 生成的视频URL
     */
    private String videoUrl;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 视频时长（秒）
     */
    private Integer duration;

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
