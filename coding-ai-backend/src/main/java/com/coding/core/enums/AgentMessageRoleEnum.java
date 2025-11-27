package com.coding.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentMessageRoleEnum {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}
