package com.coding.core.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPlanVO {
    /**
     * 计划信息
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
     * 步骤描述
     */
    private String stepDescription;

    /**
     * 步骤历史记录
     */
    private Map<String, String> history;

    /**
     * 完成百分比
     */
    private Integer percentage;

    /**
     * 是否完成
     */
    private Boolean isFinished;
}
