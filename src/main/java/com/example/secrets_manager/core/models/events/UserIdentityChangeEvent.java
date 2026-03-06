package com.example.secrets_manager.core.models.events;

import java.util.UUID;

/**
 * Marker interface for events that represent a change in a user's identity or security state,
 * requiring session revocation.
 */
public interface UserIdentityChangeEvent {
  UUID userId();
}
