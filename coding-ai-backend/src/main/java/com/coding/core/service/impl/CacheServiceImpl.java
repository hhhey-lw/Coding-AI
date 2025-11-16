package com.coding.core.service.impl;

import com.coding.core.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis服务实现
 */
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void setRemote(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public String getRemote(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteRemote(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public boolean hasRemoteKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}
