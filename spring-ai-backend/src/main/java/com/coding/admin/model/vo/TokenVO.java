package com.coding.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Token响应")
public class TokenVO {

    @Schema(description = "访问令牌（短期有效）")
    private String accessToken;

    @Schema(description = "刷新令牌（长期有效）")
    private String refreshToken;

    @Schema(description = "访问令牌过期时间（秒）")
    private Long expiresIn;

    @Schema(description = "令牌类型")
    private String tokenType = "Bearer";
}
