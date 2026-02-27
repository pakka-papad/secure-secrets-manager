package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface SecretGroupAuthorizationRepository
    extends JpaRepository<SecretGroupAuthorizationEntity, SecretGroupAuthorizationId> {

  /**
   * Finds a secret group authorization by its composite ID and acquires a pessimistic write lock on
   * the row, with a timeout.
   *
   * @param secretGroupAuthorizationId The composite ID of the authorization record.
   * @return An Optional containing the locked SecretGroupAuthorizationEntity.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") // 5 seconds
  })
  @Query("SELECT a FROM SecretGroupAuthorizationEntity a WHERE a.id = :secretGroupAuthorizationId")
  Optional<SecretGroupAuthorizationEntity> findAndLockById(
      SecretGroupAuthorizationId secretGroupAuthorizationId);
}
