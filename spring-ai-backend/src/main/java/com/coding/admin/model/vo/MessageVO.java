package com.coding.admin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class MessageVO {
    private String role;
    private String content;
    private String reasoningContent;
    private String toolCalls;
}

