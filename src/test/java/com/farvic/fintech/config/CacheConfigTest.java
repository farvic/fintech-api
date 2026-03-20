package com.farvic.fintech.config;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void shouldUseInMemoryCacheWhenRedisConnectionFactoryIsMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisConnectionFactory> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        CacheManager cacheManager = cacheConfig.cacheManager(provider);

        assertTrue(cacheManager instanceof ConcurrentMapCacheManager);
        Collection<String> cacheNames = cacheManager.getCacheNames();
        assertTrue(cacheNames.contains("accountsByUser"));
        assertTrue(cacheNames.contains("accountById"));
        assertTrue(cacheNames.contains("transactionsByAccount"));
    }

    @Test
    void shouldUseInMemoryCacheWhenRedisIsUnavailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisConnectionFactory> provider = mock(ObjectProvider.class);
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);

        when(provider.getIfAvailable()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Redis offline"));

        CacheManager cacheManager = cacheConfig.cacheManager(provider);

        assertTrue(cacheManager instanceof ConcurrentMapCacheManager);
        assertNotNull(cacheManager.getCache("accountsByUser"));
    }

    @Test
    void shouldUseRedisBackedCacheManagerWhenRedisIsReachable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisConnectionFactory> provider = mock(ObjectProvider.class);
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);

        when(provider.getIfAvailable()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        CacheManager cacheManager = cacheConfig.cacheManager(provider);

        assertFalse(cacheManager instanceof ConcurrentMapCacheManager);
        assertNotNull(cacheManager.getCache("accountsByUser"));
        assertTrue(cacheManager.getCacheNames().contains("accountsByUser"));
        assertTrue(cacheManager.getCacheNames().contains("accountById"));
        assertTrue(cacheManager.getCacheNames().contains("transactionsByAccount"));

        verify(redisConnection).ping();
        verify(redisConnection).close();
    }
}
