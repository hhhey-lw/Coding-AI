package com.coding.admin.service;

import java.util.concurrent.TimeUnit;

/**
 * Redis服务接口
 */
public interface RedisService {

    /**
     * 设置缓存
     */
    void set(String key, String value, long timeout, TimeUnit unit);

    /**
     * 获取缓存
     */
    String get(String key);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 判断key是否存在
     */
    boolean hasKey(String key);
}
