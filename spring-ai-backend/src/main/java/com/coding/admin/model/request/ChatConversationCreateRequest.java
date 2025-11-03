package com.coding.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建会话请求
 * @author coding
 * @date 2025-10-28
 */
@Data
@Schema(description = "创建会话请求")
public class ChatConversationCreateRequest {

    @Schema(description = "会话标题", example = "新的聊天")
    @NotBlank(message = "会话标题不能为空")
    private String title;

    @Schema(description = "会话ID（可选，不传则自动生成）", example = "conv_123456")
    private String id;
}

