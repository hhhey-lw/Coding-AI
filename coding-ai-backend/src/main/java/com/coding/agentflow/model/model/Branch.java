package com.coding.agentflow.model.model;

import com.coding.agentflow.model.enums.ConditionLogicEnum;
import com.coding.agentflow.model.enums.OperatorTypeEnum;
import com.coding.agentflow.model.enums.ValueTypeEnum;
import lombok.Data;

import java.util.List;

@Data
public class Branch {
    /**
     * 分支标识（可选，用于UI或调试）
     */
    private String id;

    /**
     * 目标边的 Label
     * 当条件满足时，Condition Node 会返回这个 label，
     * 引擎根据这个 label 找到对应的 Edge，从而流转到下一个节点。
     */
    private String label;

    /**
     * 条件组之间的逻辑关系
     * 例如：AND (所有条件都满足), OR (任一条件满足)
     * 默认为 AND
     */
    private ConditionLogicEnum conditionLogic;

    /**
     * 具体条件列表
     */
    private List<Condition> conditions;

    @Data
    public static class Condition {

        // --- 左操作数 ---
        /**
         * 左值
         */
        private String leftValue;

        /**
         * 左值类型
         * 默认通常是 REF
         */
        private ValueTypeEnum leftType;


        // --- 比较操作符 ---
        /**
         * GT, LT, EQ, etc.
         */
        private OperatorTypeEnum operator;


        // --- 右操作数 ---
        /**
         * 右值
         */
        private String rightValue;

        /**
         * 右值类型
         */
        private ValueTypeEnum rightType;
    }
}
