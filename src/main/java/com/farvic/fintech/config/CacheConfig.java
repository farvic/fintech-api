package com.farvic.fintech.config;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    private static final List<String> CACHE_NAMES = List.of(
            "accountsByUser",
            "accountById",
            "transactionsByAccount"
    );

    @Bean
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {
        ConcurrentMapCacheManager fallbackCacheManager = new ConcurrentMapCacheManager(CACHE_NAMES.toArray(String[]::new));
        RedisConnectionFactory redisConnectionFactory = redisConnectionFactoryProvider.getIfAvailable();

        if (redisConnectionFactory == null) {
            logger.warn("RedisConnectionFactory not found. Using in-memory cache manager.");
            return fallbackCacheManager;
        }

        if (!isRedisReachable(redisConnectionFactory)) {
            logger.warn("Redis is unavailable. Falling back to in-memory cache manager.");
            return fallbackCacheManager;
        }

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        CacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(configuration)
                .initialCacheNames(new HashSet<>(CACHE_NAMES))
                .build();

        return new CacheManager() {
            @Override
            public org.springframework.cache.Cache getCache(String name) {
                try {
                    org.springframework.cache.Cache cache = redisCacheManager.getCache(name);
                    return cache != null ? cache : fallbackCacheManager.getCache(name);
                } catch (Exception exception) {
                    logger.warn("Redis cache '{}' unavailable. Using in-memory fallback.", name);
                    return fallbackCacheManager.getCache(name);
                }
            }

            @Override
            public Set<String> getCacheNames() {
                try {
                    Set<String> names = new LinkedHashSet<>(redisCacheManager.getCacheNames());
                    names.addAll(fallbackCacheManager.getCacheNames());
                    return names;
                } catch (Exception exception) {
                    logger.warn("Redis cache names unavailable. Using in-memory fallback cache names.");
                    return new LinkedHashSet<>(fallbackCacheManager.getCacheNames());
                }
            }
        };
    }

    private boolean isRedisReachable(RedisConnectionFactory redisConnectionFactory) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.ping();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
