package com.example.secrets_manager.core.components;

import com.example.secrets_manager.core.data.CacheConstants;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationRepository;
import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.security.SecurityUtils;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Custom security expression evaluator for secret group permissions.
 *
 * <p>Optimized for high-performance by receiving context variables as arguments, minimizing SpEL
 * overhead and reflection.
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>
 * // Usage: 'principal' is automatically resolved to the user ID string
 * &#64;PreAuthorize("@groupAuth.canRead(principal, #groupId)")
 * public Secret getSecret(UUID groupId, String name) { ... }
 *
 * &#64;PreAuthorize("@groupAuth.canWrite(principal, #groupId)")
 * public void createSecret(UUID groupId, ...) { ... }
 * </pre>
 */
@Component("groupAuth")
@Slf4j
public class SecretGroupSecurityEvaluator {

  private final SecretGroupAuthorizationRepository authorizationRepository;

  private static final String SECRET_GROUP_AUTHORIZATION_CACHE_KEY = "#userId + '-' + #groupId";
  private static final int PERMISSION_READ = 1;
  private static final int PERMISSION_WRITE = 2;
  private static final int PERMISSION_DELETE = 3;

  @Autowired
  public SecretGroupSecurityEvaluator(SecretGroupAuthorizationRepository authorizationRepository) {
    this.authorizationRepository = authorizationRepository;
  }

  /** Checks if the specified user has READ permission on the group. */
  @Cacheable(
      value = CacheConstants.CACHE_SECRET_GROUP_AUTHORIZATIONS,
      key = SECRET_GROUP_AUTHORIZATION_CACHE_KEY)
  public boolean canRead(String userId, UUID groupId) {
    return hasPermission(userId, groupId, PERMISSION_READ);
  }

  /** Checks if the specified user has WRITE permission on the group. */
  @Cacheable(
      value = CacheConstants.CACHE_SECRET_GROUP_AUTHORIZATIONS,
      key = SECRET_GROUP_AUTHORIZATION_CACHE_KEY)
  public boolean canWrite(String userId, UUID groupId) {
    return hasPermission(userId, groupId, PERMISSION_WRITE);
  }

  /** Checks if the specified user has DELETE permission on the group. */
  @Cacheable(
      value = CacheConstants.CACHE_SECRET_GROUP_AUTHORIZATIONS,
      key = SECRET_GROUP_AUTHORIZATION_CACHE_KEY)
  public boolean canDelete(String userId, UUID groupId) {
    return hasPermission(userId, groupId, PERMISSION_DELETE);
  }

  private boolean hasPermission(String userIdString, UUID groupId, int permissionType) {
    // 1. Administrators bypass group-level checks
    if (SecurityUtils.hasRole(UserRole.ADMIN)) {
      return true;
    }

    if (userIdString == null || groupId == null) {
      return false;
    }

    try {
      UUID userId = UUID.fromString(userIdString);
      var id = new SecretGroupAuthorizationId(userId, groupId);

      return authorizationRepository
          .findById(id)
          .map(
              auth ->
                  switch (permissionType) {
                    case PERMISSION_READ -> auth.isPRead();
                    case PERMISSION_WRITE -> auth.isPWrite();
                    case PERMISSION_DELETE ->
                        (auth.isPDelete() && SecurityUtils.hasRole(UserRole.SECRET_MANAGER));
                    default -> false;
                  })
          .orElse(false);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid UUID format for userId forensics: {}", userIdString);
      return false;
    }
  }
}
