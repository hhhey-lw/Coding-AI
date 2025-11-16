package com.coding.workflow.model.request;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 图像生成请求模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationRequest {

    /**
     * 供应商名称
     */
    private String provider;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 生成提示词
     */
    private String prompt;

    /**
     * 图像尺寸
     */
    private String size;

    /**
     * 参考图像URL列表
     */
    private List<String> imageUrls;

    /**
     * 最大生成图片数量
     */
    private Integer maxImages;

    /**
     * 是否启用水印
     */
    private Boolean watermark;

}
