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
     * 计划创建
     */
    PLAN_CREATED,
    /**
     * 步骤执行
     */
    STEP_EXECUTION,
    /**
     * 步骤完成
     */
    STEP_COMPLETE,
    /**
     * 思考过程
     */
    THOUGHT,
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
    /**
     * 回答
     */
    ANSWER,
    ;
}
