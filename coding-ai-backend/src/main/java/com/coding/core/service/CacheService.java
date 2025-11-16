package com.coding.core.service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口
 */
public interface CacheService {

    // ----------------- 远程缓存相关接口 -----------------

    /**
     * 设置远程缓存
     */
    void setRemote(String key, String value, long timeout, TimeUnit unit);

    /**
     * 获取远程缓存
     */
    String getRemote(String key);

    /**
     * 删除远程缓存
     */
    void deleteRemote(String key);

    /**
     * 判断远程缓存key是否存在
     */
    boolean hasRemoteKey(String key);

    // ----------------- 本地缓存相关接口 -----------------
}
