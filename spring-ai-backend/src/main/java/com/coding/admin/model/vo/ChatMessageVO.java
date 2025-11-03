package com.coding.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 聊天消息VO
 * @author coding
 * @date 2025-10-28
 */
@Data
@Schema(description = "聊天消息VO")
public class ChatMessageVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会话ID")
    private String conversationId;

    @Schema(description = "消息内容（JSON格式）")
    private String messages;

    @Schema(description = "消息类型")
    private String type;
}

