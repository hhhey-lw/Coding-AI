package com.coding.agentflow.utils;

import com.coding.agentflow.model.enums.OperatorTypeEnum;
import com.coding.agentflow.model.model.Branch;
import com.coding.agentflow.model.enums.ConditionLogicEnum;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 条件评估器，用于评估单条条件
 */
public class ConditionEvaluator {

    /**
     * 评估整个分支是否满足条件
     *
     * @param branch 分支配置
     * @param state  当前上下文状态
     * @return 是否满足
     */
    public static boolean evaluateBranch(Branch branch, Map<String, Object> state) {
        // 如果没有条件，通常作为默认分支(Else)，直接返回 true
        if (branch.getConditions() == null || branch.getConditions().isEmpty()) {
            return true;
        }

        // 获取逻辑关系，默认为 AND
        ConditionLogicEnum logic = branch.getConditionLogic();
        if (logic == null) {
            logic = ConditionLogicEnum.AND;
        }

        boolean isAnd = (logic == ConditionLogicEnum.AND);

        for (Branch.Condition condition : branch.getConditions()) {
            boolean result = evaluate(condition, state);

            if (isAnd) {
                // AND 模式：只要有一个 false，整体即为 false
                if (!result) {
                    return false;
                }
            } else {
                // OR 模式：只要有一个 true，整体即为 true
                if (result) {
                    return true;
                }
            }
        }

        // 循环结束后的判定：
        // AND 模式：若未提前返回 false，说明所有条件都满足 -> true
        // OR  模式：若未提前返回 true， 说明所有条件都不满足 -> false
        return isAnd;
    }

    public static boolean evaluate(Branch.Condition condition, Map<String, Object> state) {
        // 1. 解析左右值
        Object leftVal = resolveValue(condition.getLeftValue(), state);
        Object rightVal = resolveValue(condition.getRightValue(), state);

        if (leftVal == null || rightVal == null) {
            // 简单的空值处理策略
            if (condition.getOperator() == null) {
                return false;
            }
            
            switch (condition.getOperator()) {
                case IS_EMPTY:
                    return leftVal == null || leftVal.toString().isEmpty();
                case NOT_EMPTY:
                    return leftVal != null && !leftVal.toString().isEmpty();
                case EQUALS:
                    return Objects.equals(leftVal, rightVal);
                case NOT_EQUALS:
                    return !Objects.equals(leftVal, rightVal);
                default:
                    return false;
            }
        }

        // 2. 执行比较
        switch (condition.getOperator()) {
            case EQUALS:
                return compareEquals(leftVal, rightVal);
            case NOT_EQUALS:
                return !compareEquals(leftVal, rightVal);
            case IS_EMPTY:
                return leftVal.toString().isEmpty();
            case NOT_EMPTY:
                return !leftVal.toString().isEmpty();
            case CONTAINS:
                return leftVal.toString().contains(rightVal.toString());
            case NOT_CONTAINS:
                return !leftVal.toString().contains(rightVal.toString());
            case STARTS_WITH:
                return leftVal.toString().startsWith(rightVal.toString());
            case ENDS_WITH:
                return leftVal.toString().endsWith(rightVal.toString());
            case REGEX:
                return Pattern.matches(rightVal.toString(), leftVal.toString());
            // 数值比较
            default:
                return compareNumeric(leftVal, rightVal, condition.getOperator());
        }
    }

    /**
     * 解析变量值
     * 支持两种格式：
     * 1. {{nodeId.key}} - 命名空间格式，从特定节点获取值
     * 2. {{key}} - 全局格式（向后兼容）
     * 3. 普通字符串 - 直接返回字面量
     */
    private static Object resolveValue(String value, Map<String, Object> state) {
        if (value == null) {
            return null;
        }
        
        // 检查是否是模板变量格式 {{xxx}}
        if (value.startsWith("{{") && value.endsWith("}}")) {
            String variableName = value.substring(2, value.length() - 2).trim();
            
            // 直接查找（支持命名空间格式 nodeId.key）
            if (state.containsKey(variableName)) {
                return state.get(variableName);
            }
            
            // 如果是命名空间格式但未找到，返回 null
            if (variableName.contains(".")) {
                return null;
            }
            
            // 向后兼容：尝试在所有命名空间中查找该 key
            for (Map.Entry<String, Object> entry : state.entrySet()) {
                String stateKey = entry.getKey();
                if (stateKey.endsWith("." + variableName)) {
                    return entry.getValue();
                }
            }
            
            return null;
        }
        
        // 普通字符串，直接返回字面量
        return value;
    }

    private static boolean compareEquals(Object left, Object right) {
        try {
            BigDecimal l = new BigDecimal(left.toString());
            BigDecimal r = new BigDecimal(right.toString());
            return l.compareTo(r) == 0;
        } catch (Exception e) {
            return left.toString().equals(right.toString());
        }
    }

    private static boolean compareNumeric(Object left, Object right, OperatorTypeEnum op) {
        try {
            BigDecimal l = new BigDecimal(left.toString());
            BigDecimal r = new BigDecimal(right.toString());
            int result = l.compareTo(r);
            
            switch (op) {
                case EQUALS: return result == 0;
                case NOT_EQUALS: return result != 0;
                case GT: return result > 0;
                case GTE: return result >= 0;
                case LT: return result < 0;
                case LTE: return result <= 0;
                default: return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
