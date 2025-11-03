package com.coding.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DefaultStatusEnum {
    DELETED(0, "deleted", "删除态"),
    ACTIVE(1, "active", "活跃态")
    ;

    private final int code;
    private final String enCode;
    private final String description;

}
