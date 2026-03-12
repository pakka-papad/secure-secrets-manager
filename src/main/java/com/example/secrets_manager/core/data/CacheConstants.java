package com.example.secrets_manager.core.data;

/**
 * Centralized constant definitions for all system-wide cache names. Used to ensure consistency
 * across configuration, listeners, and filters.
 */
public final class CacheConstants {

  private CacheConstants() {
    // Prevent instantiation
  }

  /**
   * Rate limiting buckets for authentication endpoints (e.g., login, refresh). Usually keyed by IP
   * Address.
   */
  public static final String CACHE_AUTH_BUCKETS = "auth-buckets";

  /** Rate limiting buckets for general API endpoints. Usually keyed by authenticated User ID. */
  public static final String CACHE_GENERAL_API_BUCKETS = "general-api-buckets";

  /** Revocation timestamps for access tokens. Keyed by User ID. */
  public static final String CACHE_USER_REVOCATIONS = "user-revocations";

  /** Authorization flags for users on secret groups. Keyed by composite User-Group ID. */
  public static final String CACHE_SECRET_GROUP_AUTHORIZATIONS = "secret-group-authorizations";
}
