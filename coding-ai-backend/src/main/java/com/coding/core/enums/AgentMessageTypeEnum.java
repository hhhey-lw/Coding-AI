package com.coding.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentMessageTypeEnum {
    /**
     * 计划进度
     */
    PLAN_PROGRESS,
    /**
     * 步骤执行
     */
    STEP_EXECUTION,
    /**
     * 工具调用
     */
    TOOL_CALL,
    /**
     * 工具响应
     */
    TOOL_RESPONSE,
    /**
     * 对话结束
     */
    STREAM_END,
    ;
}
