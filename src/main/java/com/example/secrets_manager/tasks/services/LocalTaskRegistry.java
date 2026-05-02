package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.events.TaskStartedEvent;
import com.example.secrets_manager.tasks.models.events.TaskStoppedEvent;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * In-memory registry of tasks currently being processed by THIS instance. Updates are driven by
 * Spring Application Events.
 */
@Component
public class LocalTaskRegistry {

  private final Set<UUID> activeTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

  @EventListener
  public void onTaskStarted(TaskStartedEvent event) {
    activeTasks.add(event.taskId());
  }

  @EventListener
  public void onTaskStopped(TaskStoppedEvent event) {
    activeTasks.remove(event.taskId());
  }

  /** Checks if this worker is currently processing any tasks. */
  public boolean hasActiveTasks() {
    return !activeTasks.isEmpty();
  }

  /** Returns the count of active tasks on this worker. */
  public int getActiveTaskCount() {
    return activeTasks.size();
  }
}
