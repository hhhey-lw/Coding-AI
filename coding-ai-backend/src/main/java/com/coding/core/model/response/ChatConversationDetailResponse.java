package com.coding.core.model.response;

import com.coding.core.model.vo.ChatConversationVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话详情响应（包含消息列表）
 * @author coding
 * @date 2025-10-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话详情响应")
public class ChatConversationDetailResponse {

    @Schema(description = "会话信息")
    private ChatConversationVO conversation;

    @Schema(description = "消息列表")
    private List<AiChatMessageVO> messages;
}

