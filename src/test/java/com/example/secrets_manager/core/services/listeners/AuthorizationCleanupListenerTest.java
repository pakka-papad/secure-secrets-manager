package com.example.secrets_manager.core.services.listeners;

import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.CacheConstants;
import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationRepository;
import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.core.models.events.UserDeletedEvent;
import com.example.secrets_manager.core.models.events.UserRolesUpdatedEvent;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class AuthorizationCleanupListenerTest {

  @Mock private SecretGroupAuthorizationRepository repository;
  @Mock private CacheManager cacheManager;
  @Mock private org.springframework.cache.Cache springCache;
  @Mock private Cache<Object, Object> nativeCache;

  @InjectMocks private AuthorizationCleanupListener listener;

  @BeforeEach
  void setUp() {
    when(cacheManager.getCache(CacheConstants.CACHE_SECRET_GROUP_AUTHORIZATIONS))
        .thenReturn(springCache);
    when(springCache.getNativeCache()).thenReturn(nativeCache);
  }

  @Test
  @SuppressWarnings("unchecked")
  void onUserDeleted_ShouldDeleteAuthorizationsAndEvictCache() {
    // Given
    UUID userId = UUID.randomUUID();
    Cache.Entry<Object, Object> entry = mock(Cache.Entry.class);
    when(entry.getKey()).thenReturn(userId + "-group1");
    when(nativeCache.iterator()).thenReturn(List.of(entry).iterator());

    // When
    listener.onUserDeleted(new UserDeletedEvent(userId));

    // Then
    verify(repository).deleteByIdUserId(userId);
    verify(nativeCache).remove(userId + "-group1");
  }

  @Test
  @SuppressWarnings("unchecked")
  void onUserRolesUpdated_WhenDemotedFromSecretManager_ShouldRevokeDeleteAndEvictCache() {
    // Given
    UUID userId = UUID.randomUUID();
    var oldRoles = EnumSet.of(UserRole.SECRET_MANAGER, UserRole.USER);
    var newRoles = EnumSet.of(UserRole.USER);

    Cache.Entry<Object, Object> entry = mock(Cache.Entry.class);
    when(entry.getKey()).thenReturn(userId + "-group1");
    when(nativeCache.iterator()).thenReturn(List.of(entry).iterator());

    // When
    listener.onUserRolesUpdated(new UserRolesUpdatedEvent(userId, oldRoles, newRoles));

    // Then
    verify(repository).revokeDeletePermissionForUser(userId);
    verify(nativeCache).remove(userId + "-group1");
  }

  @Test
  @SuppressWarnings("unchecked")
  void onUserRolesUpdated_WhenDemotedFromAdmin_ShouldRevokeDeleteAndEvictCache() {
    // Given
    UUID userId = UUID.randomUUID();
    var oldRoles = EnumSet.of(UserRole.ADMIN, UserRole.USER);
    var newRoles = EnumSet.of(UserRole.USER);

    Cache.Entry<Object, Object> entry = mock(Cache.Entry.class);
    when(entry.getKey()).thenReturn(userId + "-group1");
    when(nativeCache.iterator()).thenReturn(List.of(entry).iterator());

    // When
    listener.onUserRolesUpdated(new UserRolesUpdatedEvent(userId, oldRoles, newRoles));

    // Then
    verify(repository).revokeDeletePermissionForUser(userId);
    verify(nativeCache).remove(userId + "-group1");
  }

  @Test
  @SuppressWarnings("unchecked")
  void onUserRolesUpdated_WhenNoDemotion_ShouldOnlyEvictCache() {
    // Given
    UUID userId = UUID.randomUUID();
    var oldRoles = EnumSet.of(UserRole.USER);
    var newRoles = EnumSet.of(UserRole.USER, UserRole.SECRET_MANAGER); // Promotion

    Cache.Entry<Object, Object> entry = mock(Cache.Entry.class);
    when(entry.getKey()).thenReturn(userId + "-group1");
    when(nativeCache.iterator()).thenReturn(List.of(entry).iterator());

    // When
    listener.onUserRolesUpdated(new UserRolesUpdatedEvent(userId, oldRoles, newRoles));

    // Then
    verify(repository, never()).revokeDeletePermissionForUser(any());
    verify(nativeCache).remove(userId + "-group1");
  }
}
