package com.coding.core.model.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息Model
 * @author coding
 * @date 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天消息Model")
public class ChatMessageModel {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会话ID")
    private String conversationId;

    @Schema(description = "消息体")
    private String messages;

    @Schema(description = "消息类型")
    private String type;
}

