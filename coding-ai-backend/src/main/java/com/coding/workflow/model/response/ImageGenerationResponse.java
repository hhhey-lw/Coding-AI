package com.coding.workflow.model.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 图像生成响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 生成的图像URL列表
     */
    private List<String> imageUrls;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误码
     */
    private String errorCode;
}
