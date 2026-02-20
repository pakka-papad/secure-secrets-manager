package com.example.secrets_manager.security;

import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.utils.CoreUtils;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Custom AuthenticationProvider that uses CryptographyService to verify passwords. This allows us
 * to support multiple hashing algorithms and access raw byte arrays.
 */
@Component
@Slf4j
public class CryptoAuthenticationProvider implements AuthenticationProvider {

  private final UserDetailsService userDetailsService;
  private final CryptographyService cryptographyService;
  private final ObjectMapper objectMapper;

  @Autowired
  public CryptoAuthenticationProvider(
      UserDetailsService userDetailsService,
      CryptographyService cryptographyService,
      ObjectMapper objectMapper) {
    this.userDetailsService = userDetailsService;
    this.cryptographyService = cryptographyService;
    this.objectMapper = objectMapper;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = (String) authentication.getCredentials();

    // 1. Load the user details wrapper
    AppUserDetails userDetails;
    try {
      userDetails = (AppUserDetails) userDetailsService.loadUserByUsername(username);
    } catch (UsernameNotFoundException e) {
      // User not found in DB - throw exception with null userId
      throw new DetailedAuthenticationException(null, "Invalid username or password", e);
    }

    User user = userDetails.getUser();

    // 2. Prepare the stored hash data for verification
    HashedPassword storedHash =
        new HashedPassword(
            user.getPwDigest(),
            user.getPwSalt(),
            user.getHashAlgo(),
            CoreUtils.jsonStringToObjectMap(objectMapper, user.getHashParams()));

    // 3. Verify the provided password against the stored hash
    boolean matches =
        password != null && cryptographyService.verifyPassword(password.getBytes(), storedHash);

    if (!matches) {
      // User exists but password is wrong - throw exception with the user's ID
      throw new DetailedAuthenticationException(user.getId(), "Invalid username or password");
    }

    // 4. Return fully authenticated token using the wrapper
    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
  }

  @Override
  public boolean supports(@NonNull Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
