package com.example.secrets_manager.security;

import com.example.secrets_manager.core.models.UserRole;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security-related conversions, specifically for handling role-to-authority
 * mapping and prefixing.
 */
public final class SecurityUtils {

  public static final String ROLE_PREFIX = "ROLE_";

  private SecurityUtils() {
    // Prevent instantiation
  }

  /** Retrieves the authenticated user's ID from the current security context. */
  public static UUID getAuthenticatedUserId() {
    Authentication auth =
        Objects.requireNonNull(
            SecurityContextHolder.getContext().getAuthentication(), "User is not authenticated");
    return UUID.fromString((String) Objects.requireNonNull(auth.getPrincipal()));
  }

  /**
   * Checks if the currently authenticated user has the specified role.
   *
   * @param role The role enum.
   */
  public static boolean hasRole(UserRole role) {
    if (role == null) {
      return false;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }

    String targetAuthority = prefixRole(role);
    return auth.getAuthorities().stream().anyMatch(a -> targetAuthority.equals(a.getAuthority()));
  }

  /** Prefixes a raw role name with the standard Spring Security "ROLE_" prefix. */
  public static String prefixRole(String role) {
    if (role == null) return null;
    return role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
  }

  /** Prefixes a role with the standard Spring Security "ROLE_" prefix. */
  public static String prefixRole(UserRole role) {
    if (role == null) return null;
    return ROLE_PREFIX + role.name();
  }

  /** Converts a collection of UserRole enums to a list of prefixed strings. */
  public static List<String> prefixRoles(Collection<UserRole> roles) {
    return roles.stream().map(role -> prefixRole(role.name())).collect(Collectors.toList());
  }

  /** Converts an array of role strings to a list of prefixed strings. */
  public static List<String> prefixRoles(String[] roles) {
    return Arrays.stream(roles).map(SecurityUtils::prefixRole).collect(Collectors.toList());
  }

  /** Converts a collection of UserRole enums to Spring Security GrantedAuthority objects. */
  public static List<SimpleGrantedAuthority> toAuthorities(Collection<UserRole> roles) {
    return roles.stream()
        .map(role -> new SimpleGrantedAuthority(prefixRole(role.name())))
        .collect(Collectors.toList());
  }

  /** Extracts the authority strings from a collection of GrantedAuthority objects. */
  public static List<String> getAuthoritiesAsStrings(
      Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
  }
}
