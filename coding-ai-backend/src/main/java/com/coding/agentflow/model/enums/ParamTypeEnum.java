package com.coding.agentflow.model.enums;

/**
 * 值类型，比较时进行类型转换
 * 避免类型问题比较失败：
 *  如： "9" > "60" 字符串比较
 */
public enum ParamTypeEnum {
    STRING,
    NUMBER,
    BOOLEAN
}