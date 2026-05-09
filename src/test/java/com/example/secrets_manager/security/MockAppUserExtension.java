package com.example.secrets_manager.security;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JUnit 5 extension that manages the Security Context based on the {@link WithMockAppUser}
 * annotation.
 */
public class MockAppUserExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    WithMockAppUser annotation =
        AnnotatedElementUtils.findMergedAnnotation(
            context.getRequiredTestMethod(), WithMockAppUser.class);

    if (annotation == null) {
      annotation =
          AnnotatedElementUtils.findMergedAnnotation(
              context.getRequiredTestClass(), WithMockAppUser.class);
    }

    if (annotation != null) {
      String userId =
          annotation.value().isBlank() ? UUID.randomUUID().toString() : annotation.value();

      List<SimpleGrantedAuthority> authorities =
          Arrays.stream(annotation.roles())
              .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toList());

      var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    SecurityContextHolder.clearContext();
  }
}
