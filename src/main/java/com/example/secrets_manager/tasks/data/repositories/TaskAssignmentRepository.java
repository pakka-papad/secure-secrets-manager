package com.example.secrets_manager.tasks.data.repositories;

import com.example.secrets_manager.tasks.data.entities.TaskAssignmentEntity;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignmentEntity, UUID> {

  /**
   * Atomically claims a task using a native INSERT with ON CONFLICT DO NOTHING. Returns 1 if
   * claimed successfully, 0 if already claimed by another worker.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "INSERT INTO sm.task_assignments (task_id, worker_id, assigned_at) "
              + "VALUES (:taskId, :workerId, clock_timestamp()) "
              + "ON CONFLICT (task_id) DO NOTHING",
      nativeQuery = true)
  int atomicClaim(@Param("taskId") UUID taskId, @Param("workerId") UUID workerId);

  /** Internal native query to find stale task IDs using PostgreSQL interval syntax. */
  @Query(
      value =
          "SELECT ta.task_id FROM sm.task_assignments ta "
              + "JOIN sm.worker_registry w ON ta.worker_id = w.id "
              + "WHERE w.last_heartbeat < (clock_timestamp() - CAST(:intervalStr AS interval)) "
              + "LIMIT :limit",
      nativeQuery = true)
  List<UUID> _findStaleTaskIds(@Param("intervalStr") String intervalStr, @Param("limit") int limit);

  /** Finds IDs of tasks assigned to workers whose heartbeat is older than the given Duration. */
  default List<UUID> findStaleTaskIds(Duration threshold, int limit) {
    if (threshold == null) {
      return List.of();
    }
    String intervalStr = threshold.getSeconds() + " seconds";
    return _findStaleTaskIds(intervalStr, limit);
  }

  /**
   * Deletes an assignment only if it belongs to the specified worker. This provides a "Fencing"
   * mechanism during task release.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  void deleteByTaskIdAndWorkerId(@Param("taskId") UUID taskId, @Param("workerId") UUID workerId);
}
