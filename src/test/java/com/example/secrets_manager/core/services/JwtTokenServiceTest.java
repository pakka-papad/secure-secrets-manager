package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.secrets_manager.core.models.TokenWithExpiry;
import io.jsonwebtoken.Claims;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

  private JwtTokenService jwtTokenService;
  @Mock private JwtKeyPairProvider keyPairProvider;

  @BeforeEach
  void setUp() throws Exception {
    // Generate real EC keys for testing
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
    keyPairGenerator.initialize(256);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    when(keyPairProvider.getKeyPair()).thenReturn(keyPair);

    jwtTokenService = new JwtTokenService(keyPairProvider);

    ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpirationMs", 300000L);
    ReflectionTestUtils.setField(jwtTokenService, "refreshTokenExpirationMs", 2592000000L);

    jwtTokenService.init();
  }

  @Test
  void generateAccessToken_ShouldIncludeRoles() {
    // Given
    UUID userId = UUID.randomUUID();
    List<String> roles = List.of("ROLE_ADMIN", "ROLE_USER");

    // When
    TokenWithExpiry tokenInfo = jwtTokenService.generateAccessToken(userId, roles);

    // Then
    assertThat(tokenInfo.getToken()).isNotBlank();
    assertThat(tokenInfo.getExpiry()).isAfter(java.time.Instant.now());

    Optional<Claims> claimsOpt = jwtTokenService.parseToken(tokenInfo.getToken());
    assertThat(claimsOpt).isPresent();
    Claims claims = claimsOpt.get();

    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.get("roles", List.class))
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
  }

  @Test
  void generateRefreshToken_ShouldHaveSubjectOnly() {
    // Given
    UUID userId = UUID.randomUUID();

    // When
    TokenWithExpiry tokenInfo = jwtTokenService.generateRefreshToken(userId);

    // Then
    Optional<Claims> claimsOpt = jwtTokenService.parseToken(tokenInfo.getToken());
    assertThat(claimsOpt).isPresent();
    Claims claims = claimsOpt.get();

    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.get("roles")).isNull();
  }

  @Test
  void parseToken_WithInvalidToken_ShouldReturnEmpty() {
    assertThat(jwtTokenService.parseToken("invalid.token.here")).isEmpty();
  }
}
