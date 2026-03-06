package com.example.secrets_manager.core.services.listeners;

import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.models.events.UserIdentityChangeEvent;
import com.example.secrets_manager.core.utils.CoreUtils;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener responsible for revoking all active sessions (refresh tokens and access tokens) for a
 * user when their identity or security state changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionRevocationListener {

  private final RefreshTokenRepository refreshTokenRepository;
  private final CacheManager cacheManager;

  /**
   * Listens for any UserIdentityChangeEvent to trigger a global logout and access token
   * invalidation.
   */
  @EventListener
  @Transactional
  public void onIdentityChangeEvent(UserIdentityChangeEvent event) {
    log.info("Revoking all tokens for user: {}", event.userId());

    // 1. Revoke Refresh Tokens (Database)
    refreshTokenRepository.deleteByUserId(event.userId());

    // 2. Revoke Access Tokens (Cache-based invalidation timestamp)
    Cache revocationCache = cacheManager.getCache(CoreUtils.CACHE_USER_REVOCATIONS);
    if (revocationCache != null) {
      revocationCache.put(event.userId(), Instant.now());
    }
  }
}
