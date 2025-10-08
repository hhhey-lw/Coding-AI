package com.coding.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Collection;

/**
 * 断言工具类（基于 Hutool）
 * 用于参数校验、快速失败等场景
 */
public class AssertUtil {

    // ========== Blank 相关 ==========

    /**
     * 断言字符串为 空白（null、空字符串、全空格）
     *
     * @param str     待判断字符串
     * @param message 断言失败提示信息
     */
    public static void isBlank(String str, String message) {
        if (!StrUtil.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言字符串 非空白（非 null、非空字符串、非全空格）
     *
     * @param str     待判断字符串
     * @param message 断言失败提示信息
     */
    public static void isNotBlank(String str, String message) {
        if (StrUtil.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
    }

    // ========== Null 相关 ==========

    /**
     * 断言对象为 null
     *
     * @param obj     待判断对象
     * @param message 断言失败提示信息
     */
    public static void isNull(Object obj, String message) {
        if (!ObjectUtil.isNull(obj)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言对象不为 null
     *
     * @param obj     待判断对象
     * @param message 断言失败提示信息
     */
    public static void isNotNull(Object obj, String message) {
        if (ObjectUtil.isNull(obj)) {
            throw new IllegalArgumentException(message);
        }
    }

    // ========== Empty 相关（集合） ==========

    /**
     * 断言集合为 空（null 或 size = 0）
     *
     * @param collection 待判断集合
     * @param message    断言失败提示信息
     */
    public static void isEmpty(Collection<?> collection, String message) {
        if (!CollUtil.isEmpty(collection)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言集合非空（非 null 且 size > 0）
     *
     * @param collection 待判断集合
     * @param message    断言失败提示信息
     */
    public static void isNotEmpty(Collection<?> collection, String message) {
        if (CollUtil.isEmpty(collection)) {
            throw new IllegalArgumentException(message);
        }
    }

    // ========== Boolean 相关 ==========

    /**
     * 断言条件为 true
     *
     * @param condition 条件
     * @param message   断言失败提示信息
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言条件为 false
     *
     * @param condition 条件
     * @param message   断言失败提示信息
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }
}