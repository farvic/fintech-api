package com.farvic.fintech.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class CacheConfigRedisIntegrationTest {

    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private final CacheConfig cacheConfig = new CacheConfig();

    private LettuceConnectionFactory redisConnectionFactory;

    @BeforeAll
    static void startContainer() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available");
        REDIS_CONTAINER.start();
    }

    @AfterAll
    static void stopContainer() {
        if (REDIS_CONTAINER.isRunning()) {
            REDIS_CONTAINER.stop();
        }
    }

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                REDIS_CONTAINER.getHost(),
                REDIS_CONTAINER.getMappedPort(6379));
        redisConnectionFactory = new LettuceConnectionFactory(configuration);
        redisConnectionFactory.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        if (redisConnectionFactory != null) {
            redisConnectionFactory.destroy();
        }
    }

    @Test
    void shouldReadAndWriteCacheEntriesUsingRealRedis() {
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisConnectionFactory> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisConnectionFactory);

        CacheManager cacheManager = cacheConfig.cacheManager(provider);

        assertFalse(cacheManager instanceof ConcurrentMapCacheManager);

        Cache cache = cacheManager.getCache("accountsByUser");
        assertNotNull(cache);

        String key = "user@email.com";
        String value = "cached-response";

        cache.put(key, value);

        Cache.ValueWrapper valueWrapper = cache.get(key);
        assertNotNull(valueWrapper);
        assertEquals(value, valueWrapper.get());
    }
}
