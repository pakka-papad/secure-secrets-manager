package com.example.secrets_manager.security;

import com.example.secrets_manager.core.models.UserRole;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Utility class for security-related conversions, specifically for handling role-to-authority
 * mapping and prefixing.
 */
public final class SecurityUtils {

  public static final String ROLE_PREFIX = "ROLE_";

  private SecurityUtils() {
    // Prevent instantiation
  }

  /** Prefixes a raw role name with the standard Spring Security "ROLE_" prefix. */
  public static String prefixRole(String role) {
    if (role == null) return null;
    return role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
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
