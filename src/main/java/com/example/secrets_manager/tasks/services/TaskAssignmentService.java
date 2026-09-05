package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.repositories.TaskAssignmentRepository;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentReclaimException;
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

  /**
   * Reclaims a task only if its assignment still belongs to the worker that was observed as stale.
   * The fenced release prevents delayed reapers from evicting a replacement owner.
   */
  @Transactional
  public boolean reclaimTask(UUID taskId, UUID expectedWorkerId) {
    if (TaskUtils.WORKER_ID.equals(expectedWorkerId)) {
      return false;
    }

    // 1. Ensure our worker exists before the replacement assignment is made.
    workerService.registerWorker();

    // 2. Release only the assignment that was observed as stale.
    int releasedRows = assignmentRepository.deleteByTaskIdAndWorkerId(taskId, expectedWorkerId);
    if (releasedRows != 1) {
      return false;
    }

    // 3. Claim it in the same transaction so a failed replacement rolls back the release.
    int claimedRows = assignmentRepository.atomicClaim(taskId, TaskUtils.WORKER_ID);
    if (claimedRows != 1) {
      throw new TaskAssignmentReclaimException(taskId, expectedWorkerId);
    }
    return true;
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
    assignmentRepository.deleteByTaskIdAndWorkerId(taskId, TaskUtils.WORKER_ID);
  }
}
