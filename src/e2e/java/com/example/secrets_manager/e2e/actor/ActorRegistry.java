package com.example.secrets_manager.e2e.actor;

import com.example.secrets_manager.core.models.UserRole;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Global registry tracking all human users created during the test run. */
public final class ActorRegistry {

  private static final Map<UUID, UserCredential> users = new ConcurrentHashMap<>();
  private static final Map<String, UUID> usernames = new ConcurrentHashMap<>();
  private static final Random random = new Random();

  private ActorRegistry() {}

  public static boolean isEmpty() {
    return users.isEmpty();
  }

  public static void register(UserCredential credential) {
    users.put(credential.id(), credential);
    usernames.put(credential.username(), credential.id());
  }

  public static void clear() {
    users.clear();
    usernames.clear();
  }

  public static void unregister(UUID uuid) {
    final var credential = users.remove(uuid);
    usernames.remove(credential.username());
  }

  public static UserCredential get(String username) {
    final var id = usernames.get(username);
    return users.get(id);
  }

  /** Finds any user possessing the specified role. */
  public static UserCredential findAny(UserRole role) {
    var candidates = users.values().stream().filter(u -> u.roles().contains(role)).toList();

    if (candidates.isEmpty()) {
      throw new IllegalStateException("No registered user found with role: " + role);
    }

    return candidates.get(random.nextInt(candidates.size()));
  }

  /** Updates roles for an existing user in the registry. */
  public static void updateRoles(UUID id, Set<UserRole> newRoles) {
    final var existing = users.get(id);
    if (existing != null) {
      users.put(id, new UserCredential(id, existing.username(), existing.password(), newRoles));
    }
  }

  /** Updates password for an existing user in the registry. */
  public static void updatePassword(UUID id, String newPassword) {
    final var existing = users.get(id);
    if (existing != null) {
      users.put(id, new UserCredential(id, existing.username(), newPassword, existing.roles()));
    }
  }
}
