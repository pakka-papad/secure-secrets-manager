package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.models.TokenWithExpiry;
import io.jsonwebtoken.Claims;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {

  private JwtTokenService jwtTokenService;

  @BeforeEach
  void setUp() throws Exception {
    jwtTokenService = new JwtTokenService();

    // Generate real EC keys for testing
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
    keyPairGenerator.initialize(256);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    String privBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    String pubBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

    ReflectionTestUtils.setField(jwtTokenService, "jwtPrivateKeyBase64", privBase64);
    ReflectionTestUtils.setField(jwtTokenService, "jwtPublicKeyBase64", pubBase64);
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
