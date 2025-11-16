package com.coding.core.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Plan-Execute Agent 流式事件 VO
 * 支持多种事件类型的统一封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanExecuteEventVO {
    
    /**
     * 事件类型：
     * - PLAN_CREATED: 计划创建完成
     * - PLAN_PROGRESS: 计划执行进度更新
     * - STEP_EXECUTION: 步骤执行细节（流式）
     * - STEP_COMPLETED: 步骤执行完成
     * - TOOL_CALL: 工具调用
     * - TOOL_RESULT: 工具返回结果
     * - NODE_OUTPUT: 其他节点输出
     * - STREAM_END: 流式结束信号
     */
    private String type;
    
    /**
     * 节点名称
     */
    private String node;
    
    /**
     * 计划信息（PLAN_CREATED 时使用）
     */
    private String plan;
    
    /**
     * 计划 ID
     */
    private String planId;
    
    /**
     * 当前步骤索引
     */
    private Integer currentStep;
    
    /**
     * 总步骤数
     */
    private Integer totalSteps;
    
    /**
     * 完成百分比
     */
    private Integer percentage;
    
    /**
     * 是否已完成
     */
    private Boolean isFinished;
    
    /**
     * 步骤描述
     */
    private String stepDescription;
    
    /**
     * 步骤历史记录
     */
    private Map<String, String> history;
    
    /**
     * 流式内容（STEP_EXECUTION 时使用）
     */
    private String content;
    
    /**
     * 工具调用列表（TOOL_CALL 时使用）
     */
    private List<ToolCallVO> toolCalls;
    
    /**
     * 推理内容（TOOL_CALL 时可选）
     */
    private String reasoning;
    
    /**
     * 步骤输出（STEP_COMPLETED 时使用）
     */
    private String output;
    
    /**
     * 工具返回结果（TOOL_RESULT 时使用）
     */
    private String result;
    
    /**
     * 节点数据（NODE_OUTPUT 时使用）
     */
    private Map<String, Object> data;
    
    /**
     * 消息（STREAM_END 时使用）
     */
    private String message;
}

