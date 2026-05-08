package com.example.secrets_manager.tracing;

import java.util.UUID;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * JUnit 5 extension that manages the {@link CorrelationContext} based on the {@link
 * WithCorrelationId} annotation.
 */
public class CorrelationIdExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    WithCorrelationId annotation =
        AnnotatedElementUtils.findMergedAnnotation(
            context.getRequiredTestMethod(), WithCorrelationId.class);

    if (annotation == null) {
      annotation =
          AnnotatedElementUtils.findMergedAnnotation(
              context.getRequiredTestClass(), WithCorrelationId.class);
    }

    if (annotation != null) {
      UUID correlationId;
      if (!annotation.value().isBlank()) {
        correlationId = UUID.fromString(annotation.value());
      } else {
        // Use Hibernate's UUIDv7 for consistency with the application logic
        correlationId = UuidVersion7Strategy.INSTANCE.generateUuid(null);
      }
      CorrelationContext.set(correlationId);
    }
  }

  @Override
  public void afterEach(@NonNull ExtensionContext context) {
    CorrelationContext.clear();
  }
}
