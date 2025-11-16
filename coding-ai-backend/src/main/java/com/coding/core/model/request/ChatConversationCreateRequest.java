package com.coding.core.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建会话请求
 * @author coding
 * @date 2025-10-28
 */
@Data
@Schema(description = "创建会话请求")
public class ChatConversationCreateRequest {

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "会话ID（可选）")
    private String id;
}

