package com.example.secrets_manager.security;

import com.example.secrets_manager.core.services.JwtTokenService;
import com.example.secrets_manager.core.utils.CoreUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenService jwtTokenService;
  private final CacheManager cacheManager;

  public JwtAuthenticationFilter(JwtTokenService jwtTokenService, CacheManager cacheManager) {
    this.jwtTokenService = jwtTokenService;
    this.cacheManager = cacheManager;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Try to get token from header
    getJwtFromRequest(request)
        .flatMap(jwtTokenService::parseToken) // Parse and validate signature/expiration
        .filter(this::isNotRevoked) // Check if the token was issued after the last revocation
        .ifPresent(claims -> setAuthentication(request, claims)); // Set context if valid

    // Always continue the filter chain
    filterChain.doFilter(request, response);
  }

  private boolean isNotRevoked(Claims claims) {
    String userIdStr = claims.getSubject();
    if (userIdStr == null) return false;

    UUID userId = UUID.fromString(userIdStr);
    Instant issuedAt = claims.getIssuedAt().toInstant();

    Cache revocationCache = cacheManager.getCache(CoreUtils.CACHE_USER_REVOCATIONS);
    if (revocationCache == null) return true;

    Instant lastRevocation = revocationCache.get(userId, Instant.class);
    if (lastRevocation == null) return true;

    // Token is valid ONLY if issued AFTER the last revocation event
    boolean isValid = issuedAt.isAfter(lastRevocation);
    if (!isValid) {
      log.warn(
          "Rejected revoked access token for user: {}. Issued at: {}, Revoked at: {}",
          userId,
          issuedAt,
          lastRevocation);
    }
    return isValid;
  }

  private Optional<String> getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader(SecurityConstants.JWT_HEADER);
    if (bearerToken != null && bearerToken.startsWith(SecurityConstants.JWT_TOKEN_PREFIX)) {
      return Optional.of(bearerToken.substring(SecurityConstants.JWT_TOKEN_PREFIX.length()));
    }
    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  private void setAuthentication(HttpServletRequest request, Claims claims) {
    Collection<String> roles = claims.get(SecurityConstants.JWT_CLAIM_ROLES, List.class);

    var authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());

    var authentication =
        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
