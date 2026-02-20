package com.example.secrets_manager.security;

public final class SecurityConstants {

  private SecurityConstants() {
    // Prevent instantiation
  }

  public static final String AUTH_LOGIN_URL = "/api/v1/auth/login";
  public static final String JWT_HEADER = "Authorization";
  public static final String JWT_TOKEN_PREFIX = "Bearer ";
  public static final String JWT_CLAIM_ROLES = "roles";

  // Public API endpoints that don't require authentication
  public static final String[] PUBLIC_ENDPOINTS = {
    AUTH_LOGIN_URL, "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
  };

  // Roles
  public static final String ROLE_USER = "USER";
  public static final String ROLE_ADMIN = "ADMIN";
}
