package com.example.secrets_manager.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CorrelationContextTest {

  @AfterEach
  void tearDown() {
    CorrelationContext.clear();
  }

  @Test
  void setAndGet_ShouldWork() {
    UUID id = UUID.randomUUID();
    CorrelationContext.set(id);
    assertThat(CorrelationContext.get()).isPresent().contains(id);
  }

  @Test
  void clear_ShouldRemoveId() {
    CorrelationContext.set(UUID.randomUUID());
    CorrelationContext.clear();
    assertThat(CorrelationContext.get()).isEmpty();
  }

  @Test
  void runWithId_ShouldSetAndClearContext() {
    UUID id = UUID.randomUUID();

    CorrelationContext.runWithId(
        id,
        () -> {
          assertThat(CorrelationContext.get()).isPresent().contains(id);
        });

    assertThat(CorrelationContext.get()).isEmpty();
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
  }

  @Test
  void supplyWithId_ShouldSetAndClearContext() {
    UUID id = UUID.randomUUID();

    String result =
        CorrelationContext.supplyWithId(
            id,
            () -> {
              assertThat(CorrelationContext.get()).isPresent().contains(id);
              return "success";
            });

    assertThat(result).isEqualTo("success");
    assertThat(CorrelationContext.get()).isEmpty();
  }
}
