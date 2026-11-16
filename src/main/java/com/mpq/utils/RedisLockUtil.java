package com.mpq.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis锁工具（防缓存击穿，新增）
 */
@Component
@RequiredArgsConstructor
public class RedisLockUtil {
    private final RedisTemplate<String, Object> redisTemplate;

    // 获取锁（5秒过期）
    public boolean tryLock(String lockKey) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCK", 5, TimeUnit.SECONDS)
        );
    }

    // 释放锁
    public void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}