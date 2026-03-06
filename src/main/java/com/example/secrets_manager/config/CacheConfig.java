package com.example.secrets_manager.config;

import com.example.secrets_manager.core.data.CacheConstants;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Granular configuration for system caches. Ensures that each cache has a specific size and
 * expiration policy suited to its security requirements.
 */
@Configuration
public class CacheConfig {

  @Value("${jwt.expiration.access:300000}")
  private long accessTokenExpirationMs;

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();

    // 1. Configure the "auth-buckets" cache (Public Rate Limiting)
    cacheManager.registerCustomCache(
        CacheConstants.CACHE_AUTH_BUCKETS,
        Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(1, TimeUnit.MINUTES).build());

    // 2. Configure the "general-api-buckets" cache (Authenticated Rate Limiting)
    cacheManager.registerCustomCache(
        CacheConstants.CACHE_GENERAL_API_BUCKETS,
        Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(1, TimeUnit.MINUTES).build());

    // 3. Configure the "user-revocations" cache
    // Expiration matches access token lifetime exactly
    cacheManager.registerCustomCache(
        CacheConstants.CACHE_USER_REVOCATIONS,
        Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(accessTokenExpirationMs, TimeUnit.MILLISECONDS)
            .build());

    return cacheManager;
  }
}
