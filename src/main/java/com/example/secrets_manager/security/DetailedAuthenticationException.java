package com.example.secrets_manager.security;

import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * Custom AuthenticationException that carries the UUID of the user being targeted. This allows the
 * AuthenticationService to log the target_user_id in audit logs without performing an additional
 * database lookup.
 */
@Getter
public class DetailedAuthenticationException extends AuthenticationException {

  private final UUID userId;

  public DetailedAuthenticationException(UUID userId, String message) {
    super(message);
    this.userId = userId;
  }

  public DetailedAuthenticationException(UUID userId, String message, Throwable cause) {
    super(message, cause);
    this.userId = userId;
  }
}
