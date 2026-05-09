package com.example.secrets_manager.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationContextTest {

  private static final String MDC_KEY = "correlationId";

  @AfterEach
  void tearDown() {
    CorrelationContext.clear();
  }

  @Test
  void setAndGet_ShouldWork() {
    UUID id = UUID.randomUUID();
    CorrelationContext.set(id);
    assertThat(CorrelationContext.get()).isPresent().contains(id);
    assertThat(MDC.get(MDC_KEY)).isEqualTo(id.toString());
  }

  @Test
  void clear_ShouldRemoveIdAndMDC() {
    CorrelationContext.set(UUID.randomUUID());
    CorrelationContext.clear();
    assertThat(CorrelationContext.get()).isEmpty();
    assertThat(MDC.get(MDC_KEY)).isNull();
  }

  @Test
  void runWithId_ShouldSetAndClearContext() {
    UUID id = UUID.randomUUID();

    CorrelationContext.runWithId(
        id,
        () -> {
          assertThat(CorrelationContext.get()).isPresent().contains(id);
          assertThat(MDC.get(MDC_KEY)).isEqualTo(id.toString());
        });

    assertThat(CorrelationContext.get()).isEmpty();
    assertThat(MDC.get(MDC_KEY)).isNull();
  }

  @Test
  void runWithId_ShouldClearContextOnException() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                CorrelationContext.runWithId(
                    id,
                    () -> {
                      throw new RuntimeException("test error");
                    }))
        .isInstanceOf(RuntimeException.class);

    assertThat(CorrelationContext.get()).isEmpty();
    assertThat(MDC.get(MDC_KEY)).isNull();
  }

  @Test
  void supplyWithId_ShouldSetAndClearContext() {
    UUID id = UUID.randomUUID();

    String result =
        CorrelationContext.supplyWithId(
            id,
            () -> {
              assertThat(CorrelationContext.get()).isPresent().contains(id);
              assertThat(MDC.get(MDC_KEY)).isEqualTo(id.toString());
              return "success";
            });

    assertThat(result).isEqualTo("success");
    assertThat(CorrelationContext.get()).isEmpty();
    assertThat(MDC.get(MDC_KEY)).isNull();
  }
}
