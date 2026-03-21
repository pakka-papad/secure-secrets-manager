package com.example.secrets_manager.e2e.actor;

import com.example.secrets_manager.core.models.UserRole;
import java.util.Set;
import java.util.UUID;

/** Tracks a user's known state during E2E test execution. */
public record UserCredential(UUID id, String username, String password, Set<UserRole> roles) {}
