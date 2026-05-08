package com.example.secrets_manager.core.components;

import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.UserCreationPayload;
import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.core.services.UserService;
import com.example.secrets_manager.core.utils.CoreUtils;
import com.example.secrets_manager.tracing.CorrelationContext;
import java.util.EnumSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Automatically creates an initial administrator account upon application startup if no
 * administrators are found in the system.
 */
@Component
@Slf4j
public class BootstrapAdminProvider {

  private final UserService userService;
  private final UserRepository userRepository;

  @Value("${bootstrap.admin.username:}")
  private String initialAdminUsername;

  @Value("${bootstrap.admin.password:}")
  private String initialAdminPassword;

  @Autowired
  public BootstrapAdminProvider(UserService userService, UserRepository userRepository) {
    this.userService = userService;
    this.userRepository = userRepository;
  }

  /**
   * Listens for the application to be ready. Performs an idempotency check and creates the initial
   * admin if none exist.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (userRepository.existsByRoleAdmin()) {
      log.info("Administrator accounts already exist. Bootstrap process skipped.");
      return;
    }

    if (StringUtils.isBlank(initialAdminUsername) || StringUtils.isBlank(initialAdminPassword)) {
      log.warn(
          "No administrators found, but bootstrap credentials (bootstrap.admin.username/password) are missing. System initialization pending.");
      return;
    }

    log.info(
        "No administrators found. Initializing system with bootstrap account: {}",
        initialAdminUsername);

    // Generate a fresh UUIDv7 for this specific bootstrap run
    final var bootstrapCorrelationId = UuidVersion7Strategy.INSTANCE.generateUuid(null);

    // Wrap the entire bootstrap process in a Correlation Context
    CorrelationContext.runWithId(bootstrapCorrelationId, this::executeBootstrap);
  }

  private void executeBootstrap() {
    try {
      // 1. Establish a temporary administrative Security Context
      // We "act" as the system user and grant ourselves ROLE_ADMIN to satisfy UserService
      // guardrails.
      var bootstrapAuth =
          new UsernamePasswordAuthenticationToken(
              CoreUtils.SYSTEM_USER_ID.toString(),
              null,
              List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
      SecurityContextHolder.getContext().setAuthentication(bootstrapAuth);

      // 2. Create the first real human administrator
      userService.createUser(
          UserCreationPayload.builder()
              .name(initialAdminUsername)
              .password(initialAdminPassword.getBytes())
              .roles(EnumSet.of(UserRole.ADMIN, UserRole.USER))
              .build());

      log.info(
          "Bootstrap administrator '{}' created successfully. System initialized.",
          initialAdminUsername);

    } catch (Exception e) {
      log.error("CRITICAL: Failed to create bootstrap administrator: {}", e.getMessage(), e);
    } finally {
      // 3. Clear the temporary security context
      SecurityContextHolder.clearContext();
    }
  }
}
