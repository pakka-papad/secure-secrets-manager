package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.entities.RefreshToken;
import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.AuthResponse;
import com.example.secrets_manager.core.models.LoginPayload;
import com.example.secrets_manager.core.services.exceptions.InvalidPasswordException;
import com.example.secrets_manager.core.utils.CoreUtils;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.security.AppUserDetails;
import com.example.secrets_manager.security.DetailedAuthenticationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final JwtTokenService jwtTokenService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AuditService auditService;
  private final CryptographyService cryptographyService;

  @Autowired
  public AuthenticationService(
      AuthenticationManager authenticationManager,
      UserRepository userRepository,
      JwtTokenService jwtTokenService,
      RefreshTokenRepository refreshTokenRepository,
      AuditService auditService,
      CryptographyService cryptographyService) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.jwtTokenService = jwtTokenService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.auditService = auditService;
    this.cryptographyService = cryptographyService;
  }

  /**
   * Authenticates a user and generates a pair of access and refresh tokens.
   *
   * <p>This method performs the following steps:
   *
   * <ol>
   *   <li>Authenticates the user using the provided username and password via Spring Security.
   *   <li>Extracts the user model from the authenticated principal.
   *   <li>Generates an ECC-signed JWT access token and a refresh token.
   *   <li>Acquires a pessimistic lock on the user record to serialize auth operations.
   *   <li>Securely hashes the refresh token and persists it in the database for revocation.
   *   <li>Records a successful login event in the audit log.
   * </ol>
   *
   * @param payload The login credentials containing the username and raw password bytes.
   * @return An {@link AuthResponse} containing the generated JWT access and refresh tokens.
   * @throws InvalidPasswordException if authentication fails due to incorrect credentials.
   * @throws EntityNotFoundException if the user record is missing.
   * @throws IllegalStateException if a newly generated token is found to be invalid.
   */
  @Transactional
  public AuthResponse login(@NotNull @Valid LoginPayload payload) {
    try {
      // 1. Authenticate user via Spring Security's AuthenticationManager
      var authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  payload.getUsername(), new String(payload.getPassword())));

      // 2. Extract the User model from the authenticated Principal
      var userDetails = (AppUserDetails) authentication.getPrincipal();
      if (userDetails == null) {
        throw new EntityNotFoundException("User details not found after successful authentication");
      }
      final var userId = userDetails.getUser().getId();

      // 3. Generate tokens
      var accessTokenInfo = jwtTokenService.generateAccessToken(userId, List.of("USER"));
      var refreshTokenInfo = jwtTokenService.generateRefreshToken(userId);

      // 4. Save refresh token (for revocation) - CONCURRENT SAFE
      userRepository
          .findAndLockById(userId)
          .orElseThrow(() -> new EntityNotFoundException("User not found or deleted during login"));

      // Hash the refresh token before storage for security
      var hashedToken = cryptographyService.hashBytes(refreshTokenInfo.getToken().getBytes());

      refreshTokenRepository
          .findAndLockByUserId(userId)
          .ifPresentOrElse(
              existingToken -> {
                existingToken.setTokenHash(hashedToken.getHash());
                existingToken.setHashAlgo(hashedToken.getAlgorithm());
                existingToken.setExpiryDate(refreshTokenInfo.getExpiry());
                refreshTokenRepository.save(existingToken);
              },
              () -> {
                refreshTokenRepository.save(
                    RefreshToken.builder()
                        .userId(userId)
                        .tokenHash(hashedToken.getHash())
                        .hashAlgo(hashedToken.getAlgorithm())
                        .expiryDate(refreshTokenInfo.getExpiry())
                        .build());
              });

      // 5. Audit successful login
      auditService.save(
          AuditLogPayload.builder()
              .actorUserId(userId)
              .action(AuditAction.USER_LOGIN_SUCCESS)
              .targetUserId(userId)
              .build());

      // 6. Return response with absolute expiry timestamps
      return AuthResponse.builder()
          .accessToken(accessTokenInfo.getToken())
          .accessTokenExpiresAt(accessTokenInfo.getExpiry())
          .refreshToken(refreshTokenInfo.getToken())
          .refreshTokenExpiresAt(refreshTokenInfo.getExpiry())
          .build();

    } catch (AuthenticationException | EntityNotFoundException e) {
      UUID targetUserId = null;
      if (e instanceof DetailedAuthenticationException de) {
        targetUserId = de.getUserId();
      }

      auditService.save(
          AuditLogPayload.builder()
              .actorUserId(CoreUtils.SYSTEM_USER_ID)
              .action(AuditAction.USER_LOGIN_FAILED)
              .targetUserId(targetUserId)
              .details(
                  String.format(
                      "{\"username_attempt\":\"%s\", \"reason\":\"%s\"}",
                      payload.getUsername(), e.getMessage()))
              .build());

      throw new InvalidPasswordException("Invalid credentials.");
    }
  }
}
