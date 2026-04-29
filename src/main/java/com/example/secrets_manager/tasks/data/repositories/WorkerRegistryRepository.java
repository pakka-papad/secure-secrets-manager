package com.example.secrets_manager.tasks.data.repositories;

import com.example.secrets_manager.tasks.data.entities.WorkerRegistryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerRegistryRepository extends JpaRepository<WorkerRegistryEntity, UUID> {

  /** Atomically registers a worker or updates its heartbeat using DB-side time. */
  @Modifying
  @Query(
      value =
          "INSERT INTO sm.worker_registry (id, last_heartbeat) "
              + "VALUES (:workerId, clock_timestamp()) "
              + "ON CONFLICT (id) DO UPDATE SET last_heartbeat = EXCLUDED.last_heartbeat",
      nativeQuery = true)
  void upsertHeartbeat(@Param("workerId") UUID workerId);
}
