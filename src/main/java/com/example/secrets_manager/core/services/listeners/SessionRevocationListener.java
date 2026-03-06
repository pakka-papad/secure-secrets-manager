package com.example.secrets_manager.core.services.listeners;

import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.models.events.UserIdentityChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener responsible for revoking all active sessions (refresh tokens) for a user when their
 * identity or security state changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionRevocationListener {

  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * Listens for any UserIdentityChangeEvent to trigger a global logout. This handles password
   * updates, user deletions, and any future identity changes.
   */
  @EventListener
  @Transactional
  public void onIdentityChangeEvent(UserIdentityChangeEvent event) {
    log.info("Revoking all refresh tokens for user: {}", event.userId());
    refreshTokenRepository.deleteByUserId(event.userId());
  }
}
