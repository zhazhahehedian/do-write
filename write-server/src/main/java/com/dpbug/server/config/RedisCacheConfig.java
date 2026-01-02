package com.dpbug.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置
 *
 * <p>缓存策略：</p>
 * <ul>
 *   <li>用户API配置：10分钟 TTL（避免频繁查询数据库）</li>
 *   <li>提示词模板：24小时 TTL（相对稳定）</li>
 *   <li>会话上下文：1小时 TTL（临时数据）</li>
 * </ul>
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * 配置 Redis 缓存管理器
     *
     * @param connectionFactory Redis连接工厂
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("初始化 Redis CacheManager");

        // 默认缓存配置（10分钟 TTL）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // 自定义不同缓存的 TTL
                .withCacheConfiguration("userApiConfig", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("promptTemplate", defaultConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("sessionContext", defaultConfig.entryTtl(Duration.ofHours(1)))
                .build();
    }
}