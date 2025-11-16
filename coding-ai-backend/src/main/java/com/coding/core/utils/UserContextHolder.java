package com.coding.core.utils;

/**
 * 用户上下文持有者
 * 使用ThreadLocal存储当前线程的用户信息
 */
public class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 设置当前用户邮箱
     */
    public static void setEmail(String email) {
        EMAIL_HOLDER.set(email);
    }

    /**
     * 获取当前用户邮箱
     */
    public static String getEmail() {
        return EMAIL_HOLDER.get();
    }

    /**
     * 设置用户信息
     */
    public static void setUserInfo(Long userId, String email) {
        setUserId(userId);
        setEmail(email);
    }

    /**
     * 清除当前线程的用户信息
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        EMAIL_HOLDER.remove();
    }
}
