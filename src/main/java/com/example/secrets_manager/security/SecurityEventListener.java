package com.example.secrets_manager.security;

import com.example.secrets_manager.core.models.SecurityEvent;
import com.example.secrets_manager.core.models.SecurityEventLogPayload;
import com.example.secrets_manager.core.services.SecurityEventLogService;
import com.example.secrets_manager.security.forensics.MethodSecurityForensicRegistry;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Listens for global Spring Security events and records them in the security event log. This
 * ensures that every authorization failure is audited with rich context.
 */
@Component
@Slf4j
public class SecurityEventListener {

  private final SecurityEventLogService securityEventLogService;
  private final MethodSecurityForensicRegistry forensicRegistry;

  @Autowired
  public SecurityEventListener(
      SecurityEventLogService securityEventLogService,
      MethodSecurityForensicRegistry forensicRegistry) {
    this.securityEventLogService = securityEventLogService;
    this.forensicRegistry = forensicRegistry;
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

    var payloadBuilder =
        SecurityEventLogPayload.builder()
            .action(SecurityEvent.ACCESS_DENIED)
            .details("{\"reason\":\"Authorization denied\"}");

    // 1. Resolve the Actor ID from the authentication principal
    if (auth != null && auth.isAuthenticated()) {
      try {
        final var principal = (String) auth.getPrincipal();
        payloadBuilder.actorUserId(principal != null ? UUID.fromString(principal) : null);
      } catch (Exception e) {
        log.error("Failed to extract actor UUID from event: {}", e.getMessage());
      }
    }

    // 2. Delegate forensic extraction to the registry
    if (event.getObject() instanceof MethodInvocation invocation) {
      forensicRegistry
          .getStrategy(invocation.getMethod())
          .map(s -> s.getForensicDetails(invocation))
          .ifPresent(payloadBuilder::details);
    }

    // 3. Persist the security event (Independent Transaction)
    securityEventLogService.save(payloadBuilder.build());
  }
}
