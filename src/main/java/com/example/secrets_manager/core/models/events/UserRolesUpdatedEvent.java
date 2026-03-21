package com.example.secrets_manager.core.models.events;

import com.example.secrets_manager.core.models.UserRole;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Event published when a user's global roles are updated.
 *
 * @param userId The ID of the user whose roles were changed.
 * @param oldRoles The set of roles before the update.
 * @param newRoles The set of roles after the update.
 */
public record UserRolesUpdatedEvent(
    UUID userId, EnumSet<UserRole> oldRoles, EnumSet<UserRole> newRoles) {}
