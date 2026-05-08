package com.example.secrets_manager.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to automatically provide a Correlation ID context for a test method or class.
 *
 * <p>When used, a fresh UUIDv7 will be generated and set in the {@link CorrelationContext} before
 * the test runs, and cleared immediately after.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CorrelationIdExtension.class)
public @interface WithCorrelationId {
  /** Optional specific UUID string to use. If blank, a fresh UUIDv7 is generated. */
  String value() default "";
}
