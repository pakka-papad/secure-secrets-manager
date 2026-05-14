package com.example.secrets_manager.tasks.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.tasks.models.events.TaskStartedEvent;
import com.example.secrets_manager.tasks.models.events.TaskStoppedEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalTaskRegistryTest {

  private LocalTaskRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new LocalTaskRegistry();
  }

  @Test
  void lifecycle_ShouldTrackTasksCorrectly() {
    UUID taskId = UUID.randomUUID();

    // Initially empty
    assertThat(registry.hasActiveTasks()).isFalse();
    assertThat(registry.getActiveTaskCount()).isZero();

    // Start
    registry.onTaskStarted(new TaskStartedEvent(taskId));
    assertThat(registry.hasActiveTasks()).isTrue();
    assertThat(registry.getActiveTaskCount()).isEqualTo(1);

    // Stop
    registry.onTaskStopped(new TaskStoppedEvent(taskId));
    assertThat(registry.hasActiveTasks()).isFalse();
    assertThat(registry.getActiveTaskCount()).isZero();
  }

  @Test
  void onTaskStopped_ShouldBeIdempotent() {
    UUID taskId = UUID.randomUUID();
    registry.onTaskStopped(new TaskStoppedEvent(taskId));
    assertThat(registry.getActiveTaskCount()).isZero();
  }
}
