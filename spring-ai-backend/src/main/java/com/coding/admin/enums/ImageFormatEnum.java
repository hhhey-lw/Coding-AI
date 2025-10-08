package com.coding.admin.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 支持的图片格式枚举
 */
@Getter
@RequiredArgsConstructor
public enum ImageFormatEnum {

    JPEG("image/jpeg", "jpg"),
    JPG("image/jpg", "jpg"),
    PNG("image/png", "png");

    /**
     * MIME类型
     */
    private final String mimeType;

    /**
     * 文件扩展名
     */
    private final String extension;

    /**
     * 获取所有支持的MIME类型
     */
    public static List<String> getAllMimeTypes() {
        return Arrays.stream(values())
                .map(ImageFormatEnum::getMimeType)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有支持的文件扩展名
     */
    public static List<String> getAllExtensions() {
        return Arrays.stream(values())
                .map(ImageFormatEnum::getExtension)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 根据MIME类型判断是否支持
     */
    public static boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(format -> format.getMimeType().equalsIgnoreCase(mimeType));
    }

    /**
     * 根据文件扩展名判断是否支持
     */
    public static boolean isSupportedExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return Arrays.stream(values())
                .anyMatch(format -> format.getExtension().equalsIgnoreCase(extension));
    }
}
