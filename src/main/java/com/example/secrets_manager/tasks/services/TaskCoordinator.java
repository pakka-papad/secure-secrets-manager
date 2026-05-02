package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskAssignmentRepository;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.TaskState;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Orchestrates the polling and distributed claiming of background tasks. */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskCoordinator {

  private final TaskRepository taskRepository;
  private final TaskAssignmentRepository assignmentRepository;
  private final TaskAssignmentService assignmentService;
  private final TaskExecutorService executorService;
  private final TaskEntityConverter taskConverter;

  @Value("${tasks.polling.batch-size:50}")
  private int batchSize;

  @Value("${tasks.staleness.threshold:60s}")
  private Duration stalenessThreshold;

  /** Poll for new pending tasks. */
  @Scheduled(fixedDelayString = "${tasks.polling.pending-interval:30000}")
  public void pollPendingTasks() {
    // 1. Fetch IDs only for high-scale discovery
    List<UUID> pendingIds = taskRepository.findPendingTaskIds(TaskState.PENDING.name(), batchSize);

    for (UUID taskId : pendingIds) {
      // 2. Atomic claim attempt
      if (assignmentService.claimTask(taskId)) {
        log.info("Claimed new task {}", taskId);

        // 3. Only fetch full entity once claimed
        taskRepository
            .findById(taskId)
            .ifPresent(entity -> executorService.submitTask(taskConverter.toModel(entity)));
      }
    }
  }

  /**
   * The Reaper: Poll for stale tasks. Stale tasks are RUNNING but their assigned worker heartbeat
   * is old.
   */
  @Scheduled(fixedDelayString = "${tasks.polling.stale-interval:60000}")
  public void pollStaleTasks() {
    List<UUID> staleTaskIds = assignmentRepository.findStaleTaskIds(stalenessThreshold, batchSize);

    for (UUID taskId : staleTaskIds) {
      log.warn("Found stale task {}. Attempting to reclaim...", taskId);

      // Atomic reclaim (delete old assignment and create new one)
      if (assignmentService.reclaimTask(taskId)) {
        taskRepository
            .findById(taskId)
            .ifPresent(
                entity -> {
                  log.info("Successfully reclaimed task {}. Resubmitting...", taskId);
                  executorService.submitTask(taskConverter.toModel(entity));
                });
      }
    }
  }
}
