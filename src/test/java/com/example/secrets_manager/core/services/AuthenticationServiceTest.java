package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.RefreshToken;
import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.AuthResponse;
import com.example.secrets_manager.core.models.LoginPayload;
import com.example.secrets_manager.core.models.RefreshTokenPayload;
import com.example.secrets_manager.core.models.TokenWithExpiry;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.models.events.UserLogoutEvent;
import com.example.secrets_manager.core.services.exceptions.InvalidTokenException;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.BinaryHash;
import com.example.secrets_manager.security.AppUserDetails;
import com.example.secrets_manager.security.SecurityUtils;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock private AuthenticationManager authenticationManager;
  @Mock private UserRepository userRepository;
  @Mock private JwtTokenService jwtTokenService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private AuditService auditService;
  @Mock private SecurityEventLogService securityEventLogService;
  @Mock private CryptographyService cryptographyService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private AuthenticationService authenticationService;

  private UUID userId;
  private UserEntity mockUserEntity;
  private MockedStatic<SecurityUtils> mockedSecurityUtils;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    mockUserEntity =
        UserEntity.builder().id(userId).name("user").roles(new String[] {"USER"}).build();
    mockedSecurityUtils = mockStatic(SecurityUtils.class);
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtils.close();
  }

  @Test
  void login_ShouldReturnTokens_WhenCredentialsAreValid() {
    // Given
    LoginPayload payload = new LoginPayload("user", "pass".getBytes());
    Authentication auth = mock(Authentication.class);
    AppUserDetails userDetails = new AppUserDetails(User.builder().id(userId).build());

    when(authenticationManager.authenticate(any())).thenReturn(auth);
    when(auth.getPrincipal()).thenReturn(userDetails);
    when(jwtTokenService.generateAccessToken(any(), any()))
        .thenReturn(new TokenWithExpiry("access", Instant.now().plusSeconds(3600)));
    when(jwtTokenService.generateRefreshToken(any()))
        .thenReturn(new TokenWithExpiry("refresh", Instant.now().plusSeconds(86400)));
    when(userRepository.findAndLockById(userId)).thenReturn(Optional.of(mockUserEntity));
    when(cryptographyService.hashBytes(any()))
        .thenReturn(new BinaryHash("SHA-256", new byte[] {1}));

    // When
    AuthResponse response = authenticationService.login(payload);

    // Then
    assertThat(response.getAccessToken()).isEqualTo("access");
    assertThat(response.getRefreshToken()).isEqualTo("refresh");
    verify(refreshTokenRepository).save(any());
    verify(auditService).save(any());
  }

  @Test
  void refreshToken_ShouldRotateTokens_WhenValid() {
    // Given
    RefreshTokenPayload payload = new RefreshTokenPayload("valid-refresh-token");
    Claims claims = mock(Claims.class);
    when(jwtTokenService.parseToken(any())).thenReturn(Optional.of(claims));
    when(claims.getSubject()).thenReturn(userId.toString());
    when(userRepository.findAndLockById(userId)).thenReturn(Optional.of(mockUserEntity));

    RefreshToken storedToken =
        RefreshToken.builder().userId(userId).tokenHash(new byte[] {1}).hashAlgo("SHA-256").build();
    when(refreshTokenRepository.findByUserId(userId)).thenReturn(Optional.of(storedToken));
    when(cryptographyService.hashBytes(any()))
        .thenReturn(new BinaryHash("SHA-256", new byte[] {1}));

    when(jwtTokenService.generateAccessToken(any(), any()))
        .thenReturn(new TokenWithExpiry("new-access", Instant.now().plusSeconds(3600)));
    when(jwtTokenService.generateRefreshToken(any()))
        .thenReturn(new TokenWithExpiry("new-refresh", Instant.now().plusSeconds(86400)));

    // When
    AuthResponse response = authenticationService.refreshToken(payload);

    // Then
    assertThat(response.getAccessToken()).isEqualTo("new-access");
    assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    verify(refreshTokenRepository).save(storedToken);
  }

  @Test
  void refreshToken_ShouldThrowInvalidTokenException_WhenTokenInvalid() {
    // Given
    RefreshTokenPayload payload = new RefreshTokenPayload("invalid-token");
    when(jwtTokenService.parseToken(any())).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> authenticationService.refreshToken(payload))
        .isInstanceOf(InvalidTokenException.class);
  }

  @Test
  void logout_ShouldPublishEventAndAudit() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
    when(userRepository.findAndLockById(userId)).thenReturn(Optional.of(mockUserEntity));

    // When
    authenticationService.logout();

    // Then
    verify(eventPublisher).publishEvent(any(UserLogoutEvent.class));
    verify(auditService).save(any());
  }
}
