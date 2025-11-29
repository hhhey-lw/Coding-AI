package com.coding.agentflow.service;

import com.coding.agentflow.model.workflow.Branch;
import com.coding.agentflow.model.workflow.enums.ValueTypeEnum;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 条件评估器，用于评估单条条件
 */
public class ConditionEvaluator {

    public static boolean evaluate(Branch.Condition condition, Map<String, Object> state) {
        // 1. 解析左右值
        Object leftVal = resolveValue(condition.getLeftValue(), condition.getLeftType(), state);
        Object rightVal = resolveValue(condition.getRightValue(), condition.getRightType(), state);

        if (leftVal == null || rightVal == null) {
            // 简单的空值处理策略
            if (condition.getOperator() == null) return false;
            
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

    private static Object resolveValue(String value, ValueTypeEnum type, Map<String, Object> state) {
        if (type == ValueTypeEnum.LITERAL) {
            return value;
        }
        // REF 类型，从 state 中取
        if (value == null) return null;
        return state.get(value);
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

    private static boolean compareNumeric(Object left, Object right, com.coding.agentflow.model.workflow.enums.OperatorTypeEnum op) {
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
