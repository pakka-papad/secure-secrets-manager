package com.example.secrets_manager.security;

import com.example.secrets_manager.core.services.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenService jwtTokenService;

  public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Try to get token from header
    getJwtFromRequest(request)
        .flatMap(jwtTokenService::parseToken) // Parse and validate in one step
        .ifPresent(claims -> setAuthentication(request, claims)); // Set context if valid

    // Always continue the filter chain
    filterChain.doFilter(request, response);
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
