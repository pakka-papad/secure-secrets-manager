package com.example.secrets_manager.core.services.listeners;

import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationRepository;
import com.example.secrets_manager.core.models.events.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listener responsible for cleaning up authorization records when a user is deleted. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationCleanupListener {

  private final SecretGroupAuthorizationRepository secretGroupAuthorizationRepository;

  /** Listens for UserDeletedEvent to remove all group memberships for the user. */
  @EventListener
  @Transactional
  public void onUserDeleted(UserDeletedEvent event) {
    log.info("Cleaning up authorizations for deleted user: {}", event.userId());
    secretGroupAuthorizationRepository.deleteByIdUserId(event.userId());
  }
}
