package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.models.TokenWithExpiry;
import com.example.secrets_manager.security.SecurityConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtTokenService {

  private final JwtKeyPairProvider keyPairProvider;

  @Value("${jwt.expiration.access:300000}") // 5 minutes
  private long accessTokenExpirationMs;

  @Value("${jwt.expiration.refresh:2592000000}") // 30 days
  private long refreshTokenExpirationMs;

  private PrivateKey privateKey;
  private PublicKey publicKey;

  @Autowired
  public JwtTokenService(JwtKeyPairProvider keyPairProvider) {
    this.keyPairProvider = keyPairProvider;
  }

  @PostConstruct
  public void init() {
    var keyPair = keyPairProvider.getKeyPair();
    this.privateKey = keyPair.getPrivate();
    this.publicKey = keyPair.getPublic();
    log.info(
        "JWT EC keys initialized using provider: {}", keyPairProvider.getClass().getSimpleName());
  }

  public TokenWithExpiry generateAccessToken(UUID userId, Collection<String> roles) {
    var now = Instant.now();
    var expiryDate = now.plusMillis(accessTokenExpirationMs);
    var token =
        Jwts.builder()
            .subject(userId.toString())
            .claim(SecurityConstants.JWT_CLAIM_ROLES, roles)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact();

    return new TokenWithExpiry(token, expiryDate);
  }

  public TokenWithExpiry generateRefreshToken(UUID userId) {
    var now = Instant.now();
    var expiryDate = now.plusMillis(refreshTokenExpirationMs);
    var token =
        Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact();

    return new TokenWithExpiry(token, expiryDate);
  }

  /**
   * Parses and validates a JWT token. Combined logic to ensure signature and expiration are checked
   * in one pass.
   *
   * @param token The raw JWT string.
   * @return An Optional containing Claims if valid, or empty if validation fails.
   */
  public Optional<Claims> parseToken(String token) {
    try {
      var claims =
          Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
      return Optional.of(claims);
    } catch (ExpiredJwtException e) {
      log.warn("JWT token expired: {}", e.getMessage());
    } catch (SignatureException
        | MalformedJwtException
        | UnsupportedJwtException
        | IllegalArgumentException e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
    }
    return Optional.empty();
  }
}
