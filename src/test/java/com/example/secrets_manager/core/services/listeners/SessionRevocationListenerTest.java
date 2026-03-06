package com.example.secrets_manager.core.services.listeners;

import static org.mockito.Mockito.verify;

import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.models.events.UserDeletedEvent;
import com.example.secrets_manager.core.models.events.UserPasswordUpdatedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionRevocationListenerTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @InjectMocks private SessionRevocationListener listener;

  @Test
  void onUserDeleted_ShouldDeleteRefreshTokens() {
    UUID userId = UUID.randomUUID();
    listener.onIdentityChangeEvent(new UserDeletedEvent(userId));
    verify(refreshTokenRepository).deleteByUserId(userId);
  }

  @Test
  void onPasswordUpdated_ShouldDeleteRefreshTokens() {
    UUID userId = UUID.randomUUID();
    listener.onIdentityChangeEvent(new UserPasswordUpdatedEvent(userId));
    verify(refreshTokenRepository).deleteByUserId(userId);
  }
}
