package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
  Optional<RefreshTokenEntity> findByUserId(UUID userId);

  void deleteByUserId(UUID userId);

  /**
   * Finds a refresh token by user ID and acquires a pessimistic write lock on the row, with a
   * timeout. This is used to prevent race conditions during concurrent logins.
   *
   * @param userId The ID of the user whose refresh token is to be locked.
   * @return An Optional containing the locked RefreshToken.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
  @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.userId = :userId")
  Optional<RefreshTokenEntity> findAndLockByUserId(UUID userId);
}
