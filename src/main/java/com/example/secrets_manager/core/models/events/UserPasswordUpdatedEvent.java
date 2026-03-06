package com.example.secrets_manager.core.models.events;

import java.util.UUID;

/**
 * Domain event published when a user successfully updates their password.
 *
 * @param userId The unique identifier of the user who updated their password.
 */
public record UserPasswordUpdatedEvent(UUID userId) implements UserIdentityChangeEvent {}
