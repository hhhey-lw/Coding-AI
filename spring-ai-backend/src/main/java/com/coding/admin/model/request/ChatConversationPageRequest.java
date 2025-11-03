package com.coding.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话分页查询请求
 * @author coding
 * @date 2025-10-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "会话分页查询请求")
public class ChatConversationPageRequest extends PageRequest {

    @Schema(description = "会话状态：active-活跃, archived-已归档, deleted-已删除", example = "active")
    private String status;
}

