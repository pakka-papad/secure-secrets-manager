package com.example.secrets_manager.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that ensures every request has a Correlation ID for tracing. It manages the lifecycle of
 * the {@link CorrelationContext}.
 *
 * <p>Uses Hibernate's UUIDv7 generator to ensure time-ordered IDs, which optimizes B-Tree index
 * performance in the database.
 */
@Component
@Slf4j
public class CorrelationFilter extends OncePerRequestFilter {

  private static final String CORRELATION_HEADER = "X-Correlation-ID";

  @Value("${tracing.trust-external-id:false}")
  private boolean trustExternalId;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final var correlationId = resolveCorrelationId(request);

    try {
      // Set context (handles both ThreadLocal and MDC)
      CorrelationContext.set(correlationId);

      // Add to response header so client can trace their request
      response.setHeader(CORRELATION_HEADER, correlationId.toString());

      filterChain.doFilter(request, response);
    } finally {
      // Critical cleanup for thread-pooling safety.
      CorrelationContext.clear();
    }
  }

  private UUID resolveCorrelationId(HttpServletRequest request) {
    if (trustExternalId) {
      final var headerValue = request.getHeader(CORRELATION_HEADER);
      if (StringUtils.isNotBlank(headerValue)) {
        try {
          return UUID.fromString(headerValue);
        } catch (IllegalArgumentException e) {
          log.warn(
              "Invalid {} header received: {}. Generating new ID.",
              CORRELATION_HEADER,
              headerValue);
        }
      }
    }
    return UuidVersion7Strategy.INSTANCE.generateUuid(null);
  }
}
