package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

  /**
   * Finds the single most recent audit log entry based on the sequence ID.
   *
   * @return An Optional containing the latest AuditLogEntity, or empty if the table is empty.
   */
  Optional<AuditLogEntity> findTopByOrderBySeqIdDesc();
}
