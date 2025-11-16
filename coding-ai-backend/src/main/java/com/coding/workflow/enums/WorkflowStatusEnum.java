package com.coding.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkflowStatusEnum {
    EXECUTING("EXECUTING", "执行中"),
    FAIL("FAIL", "失败"),
    SUCCESS("SUCCESS", "成功"),
    STOP("STOP", "停止"),
    TIMEOUT("TIMEOUT", "超时"),
    ;

    private final String code;
    private final String desc;
}
