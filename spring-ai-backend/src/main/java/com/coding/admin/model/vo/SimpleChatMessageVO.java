package com.coding.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简单聊天消息VO（用于前端展示）
 * 支持 React Agent 和 Planning Agent 的完整消息格式
 * @author coding
 * @date 2025-10-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简单聊天消息VO")
public class SimpleChatMessageVO {

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息角色（user/assistant/system/tool）")
    private String role;
    
    @Schema(description = "工具调用列表（仅 assistant 消息可能有）")
    private List<ToolCallVO> toolCalls;
    
    @Schema(description = "工具响应列表（仅 tool 消息有）")
    private List<ToolResponseVO> responses;
}

