package com.example.secrets_manager.config;

import com.example.secrets_manager.core.data.CacheConstants;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Granular configuration for system caches using JCache (JSR-107) backed by Caffeine. This version
 * restores maximum size constraints using Caffeine-specific configurations.
 */
@Configuration
public class CacheConfig {

  @Value("${jwt.expiration.access:300000}")
  private long accessTokenExpirationMs;

  @Bean
  public CacheManager jCacheManager() {
    var provider = Caching.getCachingProvider(CaffeineCachingProvider.class.getName());
    var cacheManager = provider.getCacheManager();

    // 1. Configure "auth-buckets"
    cacheManager.createCache(
        CacheConstants.CACHE_AUTH_BUCKETS, createConfiguration(1, TimeUnit.MINUTES, 10000L));

    // 2. Configure "general-api-buckets"
    cacheManager.createCache(
        CacheConstants.CACHE_GENERAL_API_BUCKETS, createConfiguration(1, TimeUnit.MINUTES, 10000L));

    // 3. Configure "user-revocations"
    cacheManager.createCache(
        CacheConstants.CACHE_USER_REVOCATIONS,
        createConfiguration(accessTokenExpirationMs, TimeUnit.MILLISECONDS, 10000L));

    // 4. Configure "secret-group-authorizations"
    cacheManager.createCache(
        CacheConstants.CACHE_SECRET_GROUP_AUTHORIZATIONS,
        createConfiguration(5, TimeUnit.MINUTES, 10000L));

    return cacheManager;
  }

  /** Helper to create a Caffeine-backed JCache configuration with both TTL and Max Size. */
  private <K, V> CaffeineConfiguration<K, V> createConfiguration(
      long duration, TimeUnit unit, long maxSize) {
    var config = new CaffeineConfiguration<K, V>();
    config.setMaximumSize(OptionalLong.of(maxSize));
    config.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(unit, duration)));
    config.setManagementEnabled(true);
    config.setStatisticsEnabled(true);
    return config;
  }

  @Bean
  @Primary
  public org.springframework.cache.CacheManager springCacheManager(CacheManager jCacheManager) {
    return new JCacheCacheManager(jCacheManager);
  }
}
