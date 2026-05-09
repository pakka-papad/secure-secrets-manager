package com.example.secrets_manager.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to automatically provide a Mock Security Context for a test method or class.
 *
 * <p>When used, a UsernamePasswordAuthenticationToken will be set in the {@link
 * org.springframework.security.core.context.SecurityContextHolder} where the principal is a UUID
 * string.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MockAppUserExtension.class)
public @interface WithMockAppUser {
  /**
   * The UUID string to use as the authenticated User ID. If left blank, a random UUID will be
   * generated.
   */
  String value() default "";

  /**
   * The roles assigned to the mock user. Default is "USER". The "ROLE_" prefix will be added
   * automatically if missing.
   */
  String[] roles() default {"USER"};
}
