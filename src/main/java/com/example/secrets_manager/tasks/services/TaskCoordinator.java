package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskAssignmentRepository;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.TaskState;
import java.time.Duration;
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
  private final TaskHandlerRegistry handlerRegistry;

  @Value("${tasks.polling.batch-size:50}")
  private int batchSize;

  /**
   * Limit for over-fetching candidates. Ensures we find work even if many tasks are unsupported by
   * this specific worker.
   */
  @Value("${tasks.polling.candidate-limit:200}")
  private int candidateLimit;

  @Value("${tasks.staleness.threshold:60s}")
  private Duration stalenessThreshold;

  /** Poll for new pending tasks. */
  @Scheduled(fixedDelayString = "${tasks.polling.pending-interval:30000}")
  public void pollPendingTasks() {
    // 1. Fetch Candidates (ID + Type) for high-scale discovery
    final var candidates =
        taskRepository.findPendingCandidates(TaskState.PENDING.name(), candidateLimit);

    int claimedCount = 0;
    for (final var candidate : candidates) {
      if (claimedCount >= batchSize) break;

      // 2. Capability Guard: Only attempt to claim if we have a registered handler
      if (!handlerRegistry.isSupported(candidate.getType())) {
        continue;
      }

      // 3. Atomic claim attempt
      if (assignmentService.claimTask(candidate.getId())) {
        log.info("Claimed new task {} (Type: {})", candidate.getId(), candidate.getType());

        // 4. Only fetch full entity once claimed
        taskRepository
            .findById(candidate.getId())
            .ifPresent(entity -> executorService.submitTask(taskConverter.toModel(entity)));

        claimedCount++;
      }
    }
  }

  /**
   * The Reaper: Poll for stale tasks. Stale tasks are RUNNING but their assigned worker heartbeat
   * is old.
   */
  @Scheduled(fixedDelayString = "${tasks.polling.stale-interval:60000}")
  public void pollStaleTasks() {
    final var candidates =
        assignmentRepository.findStaleCandidates(stalenessThreshold, candidateLimit);

    int reclaimedCount = 0;
    for (final var candidate : candidates) {
      if (reclaimedCount >= batchSize) break;

      // Capability Guard: Only reclaim if we can actually finish the task
      if (!handlerRegistry.isSupported(candidate.getType())) {
        continue;
      }

      log.warn(
          "Found stale task {} (Type: {}). Reclaiming...", candidate.getId(), candidate.getType());

      // Atomic reclaim (delete old assignment and create new one)
      if (assignmentService.reclaimTask(candidate.getId())) {
        taskRepository
            .findById(candidate.getId())
            .ifPresent(
                entity -> {
                  log.info("Successfully reclaimed task {}. Resubmitting...", candidate.getId());
                  executorService.submitTask(taskConverter.toModel(entity));
                });
        reclaimedCount++;
      }
    }
  }
}
