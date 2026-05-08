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
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that ensures every request has a Correlation ID for tracing. It manages both the Java
 * CorrelationContext and the SLF4J MDC.
 *
 * <p>Uses Hibernate's UUIDv7 generator to ensure time-ordered IDs, which optimizes B-Tree index
 * performance in the database.
 */
@Component
@Slf4j
public class CorrelationFilter extends OncePerRequestFilter {

  private static final String CORRELATION_HEADER = "X-Correlation-ID";

  /**
   * Key used in SLF4J Mapped Diagnostic Context (MDC) to allow log patterns (e.g., in
   * application.yml) to automatically include the Correlation ID in every log line.
   */
  private static final String MDC_KEY = "correlationId";

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
      // Set context for both Java logic and Logging
      CorrelationContext.set(correlationId);
      MDC.put(MDC_KEY, correlationId.toString());

      // Add to response header so client can trace their request
      response.setHeader(CORRELATION_HEADER, correlationId.toString());

      // Pass control to the next filter/controller in the chain
      filterChain.doFilter(request, response);
    } finally {
      // Critical cleanup for thread-pooling safety.
      // Ensures the ID doesn't "leak" to the next user who happens to reuse this thread.
      CorrelationContext.clear();
      MDC.remove(MDC_KEY);
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
