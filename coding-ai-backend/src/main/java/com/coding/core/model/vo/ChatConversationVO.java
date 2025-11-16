package com.coding.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话VO
 * @author coding
 * @date 2025-10-28
 */
@Data
@Schema(description = "聊天会话VO")
public class ChatConversationVO {

    @Schema(description = "会话ID")
    private String id;

    @Schema(description = "发起会话的用户ID，可为空如果为匿名")
    private String userId;

    @Schema(description = "会话标题，例如用户自定义或系统生成")
    private String title;

    @Schema(description = "会话创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "会话状态：active, archived, deleted")
    private String status;
}

