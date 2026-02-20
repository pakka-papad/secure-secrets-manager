package com.example.secrets_manager.security;

import com.example.secrets_manager.core.models.User;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Wrapper class that adapts our User domain model to Spring Security's UserDetails interface. This
 * keeps the domain model clean of security-specific concerns.
 */
@Getter
public class AppUserDetails implements UserDetails {

  private final User user; // Reference to the underlying domain model

  public AppUserDetails(User user) {
    this.user = user;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority(SecurityConstants.ROLE_USER));
  }

  @Override
  public String getPassword() {
    return ""; // Verification is handled by CryptoAuthenticationProvider using raw bytes
  }

  @Override
  public String getUsername() {
    return user.getName();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return user.getDeletedAt() == null;
  }
}
