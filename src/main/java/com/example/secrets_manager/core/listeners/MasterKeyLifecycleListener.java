package com.example.secrets_manager.core.listeners;

import com.example.secrets_manager.core.models.events.MasterKeyPromotedEvent;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationInput;
import com.example.secrets_manager.tasks.services.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener for Master Key lifecycle events. Handles downstream side-effects such as background task
 * scheduling.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MasterKeyLifecycleListener {

  private final TaskService taskService;

  /**
   * Responds to a master key promotion by scheduling a background migration task. This listener
   * runs in the same transaction as the promotion, ensuring atomicity.
   */
  @EventListener
  public void onMasterKeyPromoted(MasterKeyPromotedEvent event) {
    log.info(
        "Reacting to Master Key Promotion v{}. Scheduling background migration...",
        event.newVersion());

    taskService.createTask(
        TaskType.MASTER_KEY_MIGRATION,
        MasterKeyMigrationInput.builder().targetMasterKeyVersion(event.newVersion()).build());

    log.info("Successfully scheduled migration task for Master Key v{}.", event.newVersion());
  }
}
