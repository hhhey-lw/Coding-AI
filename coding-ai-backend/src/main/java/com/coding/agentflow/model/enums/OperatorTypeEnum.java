package com.coding.agentflow.model.enums;

public enum OperatorTypeEnum {
    EQUALS, NOT_EQUALS,
    GT, GTE, LT, LTE,
    IS_EMPTY, NOT_EMPTY,
    // 下面的暂时不实现
    CONTAINS, NOT_CONTAINS,
    STARTS_WITH, ENDS_WITH,
    REGEX
}
