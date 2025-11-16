package com.coding.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelTypeEnum {
    TextGen, ImageGen, AudioGen, VideoGen, MusicGen,
    ;

    /**
     * 检查是否是有效的枚举
     */
    public static boolean isValidEnum(String modelType) {
        for (ModelTypeEnum typeEnum : values()) {
            if (typeEnum.name().equals(modelType)) {
                return true;
            }
        }
        return false;
    }
}
