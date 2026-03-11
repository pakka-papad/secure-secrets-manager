package com.example.secrets_manager.security;

import com.example.secrets_manager.core.models.SecurityEvent;
import com.example.secrets_manager.core.models.SecurityEventLogPayload;
import com.example.secrets_manager.core.services.SecurityEventLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Listens for global Spring Security events and records them in the security event log. Implements
 * "Auto-Forensics" with scrubbing and explicit @NoForensics support.
 */
@Component
@Slf4j
public class SecurityEventListener {

  private final SecurityEventLogService securityEventLogService;
  private final ObjectMapper objectMapper;
  private final ParameterNameDiscoverer parameterNameDiscoverer =
      new DefaultParameterNameDiscoverer();
  private static final Set<String> sensitiveKeywords = Set.of("password", "secret", "token", "key");

  @Autowired
  public SecurityEventListener(
      SecurityEventLogService securityEventLogService, ObjectMapper objectMapper) {
    this.securityEventLogService = securityEventLogService;
    this.objectMapper = objectMapper;
  }

  /**
   * Captures AuthorizationDeniedEvent published by Spring Security (v6+). Extracts method
   * invocation details and arguments for forensic auditing.
   */
  @EventListener
  public void onAuthorizationFailure(AuthorizationDeniedEvent<?> event) {
    final var authSupplier = event.getAuthentication();
    final var auth = authSupplier != null ? authSupplier.get() : null;
    final var username = auth != null ? auth.getName() : "anonymous";

    log.warn("Access denied for user [{}]: {}", username, event.getAuthorizationResult());

    var payloadBuilder = SecurityEventLogPayload.builder().action(SecurityEvent.ACCESS_DENIED);

    if (auth != null && auth.isAuthenticated()) {
      try {
        final var principal = (String) auth.getPrincipal();
        payloadBuilder.actorUserId(principal != null ? UUID.fromString(principal) : null);
      } catch (Exception e) {
        log.error("Failed to extract actor UUID from event: {}", e.getMessage());
      }
    }

    Map<String, Object> details = new HashMap<>();
    details.put("reason", "Authorization denied");

    if (event.getObject() instanceof MethodInvocation invocation) {
      extractMethodForensics(invocation, details);
    }

    try {
      payloadBuilder.details(objectMapper.writeValueAsString(details));
    } catch (Exception e) {
      payloadBuilder.details("{\"error\":\"Failed to serialize forensics\"}");
    }

    securityEventLogService.save(payloadBuilder.build());
  }

  private void extractMethodForensics(MethodInvocation invocation, Map<String, Object> details) {
    Method method = invocation.getMethod();
    details.put("target_class", method.getDeclaringClass().getSimpleName());
    details.put("target_method", method.getName());

    String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
    Object[] args = invocation.getArguments();
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();

    if (paramNames != null) {
      Map<String, Object> params = new HashMap<>();
      for (int i = 0; i < paramNames.length; i++) {
        String name = paramNames[i];

        // 1. Explicit Scrubbing (@NoForensics)
        if (hasNoForensicsAnnotation(parameterAnnotations[i])) {
          params.put(name, "[REDACTED_BY_ANNOTATION]");
        }
        // 2. Implicit Scrubbing (Naming convention)
        else if (isSensitive(name)) {
          params.put(name, "[REDACTED_BY_NAME]");
        }
        // 3. Safe to log
        else {
          params.put(name, args[i]);
        }
      }
      details.put("arguments", params);
    }
  }

  private boolean hasNoForensicsAnnotation(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().equals(NoForensics.class)) {
        return true;
      }
    }
    return false;
  }

  private boolean isSensitive(String paramName) {
    String lower = paramName.toLowerCase();
    return sensitiveKeywords.stream().anyMatch(lower::contains);
  }
}
