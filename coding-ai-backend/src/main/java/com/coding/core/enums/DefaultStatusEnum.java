package com.coding.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DefaultStatusEnum {
    DELETED(0, "deleted", "删除态"),
    ACTIVE(1, "active", "激活态")
    ;

    private final int code;
    private final String enCode;
    private final String description;

}
