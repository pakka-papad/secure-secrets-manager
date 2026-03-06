package com.example.secrets_manager.core.models.events;

import java.util.UUID;

/**
 * Domain event published when a user is successfully deleted from the system.
 *
 * @param userId The unique identifier of the deleted user.
 */
public record UserDeletedEvent(UUID userId) implements UserIdentityChangeEvent {}
