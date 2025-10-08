package com.coding.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "用户登录响应")
public class UserLoginVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "邮箱账号")
    private String email;

    @Schema(description = "用户昵称")
    private String userName;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "用户角色")
    private String userRole;

    @Schema(description = "访问令牌（短期有效，2小时）")
    private String accessToken;

    @Schema(description = "刷新令牌（长期有效，7天）")
    private String refreshToken;

    @Schema(description = "访问令牌过期时间（秒）")
    private Long expiresIn;

    @Schema(description = "令牌类型")
    @Builder.Default
    private String tokenType = "Bearer";
}
