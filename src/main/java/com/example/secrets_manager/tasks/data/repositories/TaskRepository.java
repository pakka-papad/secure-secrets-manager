package com.example.secrets_manager.tasks.data.repositories;

import com.example.secrets_manager.tasks.data.entities.TaskEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository
    extends JpaRepository<TaskEntity, UUID>, JpaSpecificationExecutor<TaskEntity> {

  /**
   * Finds candidates for tasks in the given state that haven't finished yet. Ordered by ID (UUIDv7
   * ensures chronological order) with a limit.
   */
  @Query(
      value =
          "SELECT id, type FROM sm.tasks "
              + "WHERE state = :state AND completed_at IS NULL "
              + "ORDER BY id ASC "
              + "LIMIT :limit",
      nativeQuery = true)
  List<TaskCandidate> findPendingCandidates(
      @Param("state") String state, @Param("limit") int limit);

  /**
   * Atomically updates task state and payloads ONLY if the given worker still owns the assignment.
   * This provides a "Fencing" mechanism against Zombie workers.
   *
   * @return Number of rows updated (1 if success, 0 if assignment lost).
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE sm.tasks "
              + "SET state = :state, "
              + "    started_at = :startedAt, "
              + "    completed_at = :completedAt, "
              + "    task_output = CAST(:taskOutput AS jsonb), "
              + "    state_extra_info = CAST(:stateExtraInfo AS jsonb) "
              + "WHERE id = :taskId AND "
              + "EXISTS (SELECT 1 FROM sm.task_assignments ta WHERE ta.task_id = :taskId AND ta.worker_id = :workerId)",
      nativeQuery = true)
  int updateFenced(
      @Param("taskId") UUID taskId,
      @Param("workerId") UUID workerId,
      @Param("state") String state,
      @Param("startedAt") Instant startedAt,
      @Param("completedAt") Instant completedAt,
      @Param("taskOutput") String taskOutput,
      @Param("stateExtraInfo") String stateExtraInfo);
}
