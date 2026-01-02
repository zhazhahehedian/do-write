package com.dpbug.server.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 分布式锁工具类
 * <p>
 * 用于防止重复提交和并发控制。
 * </p>
 *
 * @author dpbug
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 默认锁过期时间（5分钟）
     */
    private static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(5);

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁的key
     * @return true 如果成功获取锁
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_LOCK_DURATION);
    }

    /**
     * 尝试获取锁（指定过期时间）
     *
     * @param lockKey  锁的key
     * @param duration 锁的过期时间
     * @return true 如果成功获取锁
     */
    public boolean tryLock(String lockKey, Duration duration) {
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", duration);
            boolean locked = Boolean.TRUE.equals(success);
            if (locked) {
                log.debug("获取锁成功: key={}", lockKey);
            } else {
                log.debug("获取锁失败（锁已被占用）: key={}", lockKey);
            }
            return locked;
        } catch (Exception e) {
            log.error("获取锁异常: key={}, error={}", lockKey, e.getMessage());
            // 如果 Redis 不可用，允许操作继续（降级策略）
            return true;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        try {
            stringRedisTemplate.delete(lockKey);
            log.debug("释放锁成功: key={}", lockKey);
        } catch (Exception e) {
            log.error("释放锁异常: key={}, error={}", lockKey, e.getMessage());
        }
    }

    /**
     * 检查锁是否存在
     *
     * @param lockKey 锁的key
     * @return true 如果锁存在
     */
    public boolean isLocked(String lockKey) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.error("检查锁状态异常: key={}, error={}", lockKey, e.getMessage());
            return false;
        }
    }

    /**
     * 生成向导操作锁的key
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param operation 操作类型（world/characters/outlines）
     * @return 锁的key
     */
    public static String wizardLockKey(Long userId, Long projectId, String operation) {
        return String.format("wizard:lock:%d:%d:%s", userId, projectId, operation);
    }
}
