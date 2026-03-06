package com.example.secrets_manager.core.models.events;

import java.util.UUID;

/**
 * Domain event published when a user explicitly logs out of the system.
 * 
 * @param userId The unique identifier of the user who logged out.
 */
public record UserLogoutEvent(UUID userId) implements UserIdentityChangeEvent {}
