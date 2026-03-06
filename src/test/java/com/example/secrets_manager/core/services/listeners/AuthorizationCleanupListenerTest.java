package com.example.secrets_manager.core.services.listeners;

import static org.mockito.Mockito.verify;

import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationRepository;
import com.example.secrets_manager.core.models.events.UserDeletedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationCleanupListenerTest {

  @Mock private SecretGroupAuthorizationRepository repository;
  @InjectMocks private AuthorizationCleanupListener listener;

  @Test
  void onUserDeleted_ShouldDeleteAuthorizations() {
    UUID userId = UUID.randomUUID();
    listener.onUserDeleted(new UserDeletedEvent(userId));
    verify(repository).deleteByIdUserId(userId);
  }
}
