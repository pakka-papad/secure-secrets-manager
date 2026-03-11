package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterKeyRepository
    extends JpaRepository<MasterKeyEntity, Integer>, JpaSpecificationExecutor<MasterKeyEntity> {

  /**
   * Performs a bulk update of master key statuses.
   *
   * @param targetStatus The new status string to apply.
   * @param sourceStatus The status string to filter by.
   * @return The number of records updated.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE MasterKeyEntity m SET m.status = :targetStatus WHERE m.status = :sourceStatus")
  int updateStatusByStatus(String targetStatus, String sourceStatus);

  /**
   * Retrieves the highest master key version registered in the database.
   *
   * @return An Optional containing the maximum version number, or empty if no keys exist.
   */
  @Query("SELECT MAX(m.version) FROM MasterKeyEntity m")
  Optional<Integer> findMaxVersion();
}
