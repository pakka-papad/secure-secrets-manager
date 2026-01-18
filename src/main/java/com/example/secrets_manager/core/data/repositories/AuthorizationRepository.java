package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.AuthorizationEntity;
import com.example.secrets_manager.core.data.entities.AuthorizationId;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizationRepository
    extends JpaRepository<AuthorizationEntity, AuthorizationId> {

  /**
   * Finds an authorization by its composite ID and acquires a pessimistic write lock on the row,
   * with a timeout.
   *
   * @param authorizationId The composite ID of the authorization record.
   * @return An Optional containing the locked AuthorizationEntity.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") // 5 seconds
  })
  @Query("SELECT a FROM AuthorizationEntity a WHERE a.id = :authorizationId")
  Optional<AuthorizationEntity> findAndLockById(AuthorizationId authorizationId);
}
