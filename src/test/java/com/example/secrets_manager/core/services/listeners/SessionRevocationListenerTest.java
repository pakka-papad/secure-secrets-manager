package com.example.secrets_manager.core.services.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.secrets_manager.core.data.CacheConstants;
import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.models.events.UserDeletedEvent;
import com.example.secrets_manager.core.models.events.UserPasswordUpdatedEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class SessionRevocationListenerTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private CacheManager cacheManager;
  @Mock private Cache cache;
  @InjectMocks private SessionRevocationListener listener;

  @BeforeEach
  void setUp() {
    when(cacheManager.getCache(CacheConstants.CACHE_USER_REVOCATIONS)).thenReturn(cache);
  }

  @Test
  void onUserDeleted_ShouldDeleteRefreshTokensAndRecordRevocation() {
    UUID userId = UUID.randomUUID();
    listener.onIdentityChangeEvent(new UserDeletedEvent(userId));

    verify(refreshTokenRepository).deleteByUserId(userId);
    verify(cache).put(eq(userId), any());
  }

  @Test
  void onPasswordUpdated_ShouldDeleteRefreshTokensAndRecordRevocation() {
    UUID userId = UUID.randomUUID();
    listener.onIdentityChangeEvent(new UserPasswordUpdatedEvent(userId));

    verify(refreshTokenRepository).deleteByUserId(userId);
    verify(cache).put(eq(userId), any());
  }
}
