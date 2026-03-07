package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.UserEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository
    extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
  Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<UserEntity> findAllByDeletedAtIsNull();

  Optional<UserEntity> findByNameAndDeletedAtIsNull(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
  @Query("SELECT u FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
  Optional<UserEntity> findAndLockById(UUID id);

  /**
   * Checks if any active (non-deleted) human administrator exists. This excludes the internal
   * 'system' user.
   */
  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM sm.users WHERE 'ADMIN' = ANY(roles) AND id != '00000000-0000-0000-0000-000000000000' AND deleted_at IS NULL)",
      nativeQuery = true)
  boolean existsByRoleAdmin();

  /** Counts the number of active human administrators. */
  @Query(
      value =
          "SELECT COUNT(*) FROM sm.users WHERE 'ADMIN' = ANY(roles) AND id != '00000000-0000-0000-0000-000000000000' AND deleted_at IS NULL",
      nativeQuery = true)
  long countActiveAdmins();
}
