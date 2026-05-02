package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.repositories.TaskAssignmentRepository;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing distributed task assignments and ownership. */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskAssignmentService {

  private final TaskAssignmentRepository assignmentRepository;
  private final WorkerService workerService;

  /**
   * Atomically claims a task for this worker. Registers the worker lazily if this is its first
   * task.
   *
   * @param taskId The task to claim.
   * @return true if claimed successfully, false otherwise.
   */
  @Transactional
  public boolean claimTask(UUID taskId) {
    // 1. Ensure worker exists in registry before creating assignment (FK requirement)
    workerService.registerWorker();

    // 2. Atomic claim attempt
    int updatedRows = assignmentRepository.atomicClaim(taskId, TaskUtils.WORKER_ID);
    return updatedRows == 1;
  }

  /** Reclaims a task from a stale worker. */
  @Transactional
  public boolean reclaimTask(UUID taskId) {
    // 1. Force release the stale assignment
    assignmentRepository.deleteById(taskId);
    assignmentRepository.flush();

    // 2. Try to claim it for ourselves (handles worker registration)
    return claimTask(taskId);
  }

  /** Confirms that THIS instance still owns the task assignment. Used for "Zombie" protection. */
  @Transactional(readOnly = true)
  public boolean isAssignmentStillValid(UUID taskId) {
    return assignmentRepository
        .findById(taskId)
        .map(a -> a.getWorkerId().equals(TaskUtils.WORKER_ID))
        .orElse(false);
  }

  /** Removes the assignment upon completion or failure. */
  @Transactional
  public void releaseTask(UUID taskId) {
    assignmentRepository.deleteById(taskId);
  }
}
