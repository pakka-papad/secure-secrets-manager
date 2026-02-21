package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.UserEntityConverter;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.security.AppUserDetails;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private static final Set<String> blockedUsernames = Set.of("system");

  private final UserRepository userRepository;

  @Autowired
  public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Loads user-specific data by username. This method is used by Spring Security's
   * AuthenticationManager to verify user credentials.
   *
   * @param username The username (name) of the user to load.
   * @return A UserDetails object (AppUserDetails wrapping the User model) that Spring Security can
   *     use for authentication.
   * @throws UsernameNotFoundException if the user does not exist or is soft-deleted.
   */
  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String username)
      throws UsernameNotFoundException {
    final var isBlocked =
        blockedUsernames.stream().anyMatch(invalidName -> invalidName.equalsIgnoreCase(username));
    if (isBlocked) {
      throw new UsernameNotFoundException(String.format("User not found: %s", username));
    }

    return userRepository
        .findByNameAndDeletedAtIsNull(username)
        .map(UserEntityConverter::toModel)
        .map(AppUserDetails::new)
        .orElseThrow(
            () -> new UsernameNotFoundException(String.format("User not found: %s", username)));
  }
}
