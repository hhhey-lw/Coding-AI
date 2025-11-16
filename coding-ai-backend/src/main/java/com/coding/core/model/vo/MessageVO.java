package com.coding.core.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MessageVO {
    /**
     * 消息角色，如 user、assistant 等
     */
    private String role;
    /**
     * 消息内容
     */
    private String content;
    /**
     * 推理过程内容
     */
    private String reasoningContent;
    /**
     * 工具调用信息
     */
    private String toolCalls;
}

