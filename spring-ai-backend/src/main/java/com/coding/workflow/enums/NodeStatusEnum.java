package com.coding.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeStatusEnum {
    SUCCESS("success", "成功"),
    FAIL("fail", "失败"),
    SKIP("skip", "跳过"),
    EXECUTING("executing", "执行中"),
    PAUSE("pause", "暂停"),
    STOP("stop", "停止"),;

    private final String code;

    private final String desc;
}
