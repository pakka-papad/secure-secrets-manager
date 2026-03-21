package com.example.secrets_manager.core.services.listeners;

import com.example.secrets_manager.core.data.CacheConstants;
import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationRepository;
import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.core.models.events.UserDeletedEvent;
import com.example.secrets_manager.core.models.events.UserRolesUpdatedEvent;
import java.util.UUID;
import javax.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listener responsible for cleaning up authorization records based on user life-cycle events. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationCleanupListener {

  private final SecretGroupAuthorizationRepository secretGroupAuthorizationRepository;
  private final CacheManager cacheManager;

  /** Listens for UserDeletedEvent to remove all group memberships for the user. */
  @EventListener
  @Transactional
  public void onUserDeleted(UserDeletedEvent event) {
    log.info("Cleaning up authorizations for deleted user: {}", event.userId());
    secretGroupAuthorizationRepository.deleteByIdUserId(event.userId());
    evictUserAuthorizations(event.userId());
  }

  /**
   * Listens for UserRolesUpdatedEvent to surgically revoke permissions that require specific global
   * roles (e.g. DELETE requires SECRET_MANAGER).
   */
  @EventListener
  @Transactional
  public void onUserRolesUpdated(UserRolesUpdatedEvent event) {
    boolean possiblyHadDeletePermission =
        event.oldRoles().contains(UserRole.SECRET_MANAGER)
            || event.oldRoles().contains(UserRole.ADMIN);
    boolean cannotHaveDeletePermission =
        !event.newRoles().contains(UserRole.SECRET_MANAGER)
            && !event.newRoles().contains(UserRole.ADMIN);

    if (possiblyHadDeletePermission && cannotHaveDeletePermission) {
      log.info("User {} demoted. Revoking all group-level DELETE permissions.", event.userId());
      secretGroupAuthorizationRepository.revokeDeletePermissionForUser(event.userId());
    }

    // Always evict cache on role update to ensure evaluator picks up the new global role state
    evictUserAuthorizations(event.userId());
  }

  /**
   * Performs prefix-based eviction on the authorizations cache. Scans for keys matching the pattern
   * "{userId}-*" and removes them.
   */
  @SuppressWarnings("unchecked")
  private void evictUserAuthorizations(UUID userId) {
    var springCache = cacheManager.getCache(CacheConstants.CACHE_SECRET_GROUP_AUTHORIZATIONS);
    if (springCache == null) {
      return;
    }

    // Since we use JCache (JSR-107), the native cache is javax.cache.Cache
    var nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
    String prefix = userId.toString() + "-";

    log.debug("Evicting cache entries for user: {}", userId);

    // Iterate and remove matching keys.
    // JCache iterator is safe for concurrent modification during removal in most providers
    // (including Caffeine).
    for (Cache.Entry<Object, Object> entry : nativeCache) {
      String key = entry.getKey().toString();
      if (key.startsWith(prefix)) {
        nativeCache.remove(entry.getKey());
      }
    }
  }
}
