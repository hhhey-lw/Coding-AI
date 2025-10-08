package com.coding.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Ai模型BaseVO")
public class AiModelBaseVO {
    @Schema(name = "提供商")
    private String provider;
    @Schema(name = "提供商名称")
    private String providerName;
    @Schema(name = "模型类型", description = "TextGen, MusicGen, VideoGen, ImageGen")
    private String modelType;
    @Schema(name = "模型标识")
    private String modelId;
}
