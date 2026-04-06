package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.utils.CoreUtils;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository
    extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

  @Query("SELECT u FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
  Optional<UserEntity> _findByIdAndDeletedAtIsNull(@Param("id") UUID id);

  default Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id) {
    if (id == null || CoreUtils.SYSTEM_USER_ID.equals(id)) {
      return Optional.empty();
    }
    return _findByIdAndDeletedAtIsNull(id);
  }

  @Query(
      "SELECT u FROM UserEntity u WHERE u.name = :name AND u.deletedAt IS NULL AND u.id != :systemId")
  Optional<UserEntity> findByNameAndDeletedAtIsNull(
      @Param("name") String name, @Param("systemId") UUID systemId);

  default Optional<UserEntity> findByNameAndDeletedAtIsNull(String name) {
    return findByNameAndDeletedAtIsNull(name, CoreUtils.SYSTEM_USER_ID);
  }

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
  @Query("SELECT u FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
  Optional<UserEntity> _findAndLockById(@Param("id") UUID id);

  default Optional<UserEntity> findAndLockById(UUID id) {
    if (id == null || CoreUtils.SYSTEM_USER_ID.equals(id)) {
      return Optional.empty();
    }
    return _findAndLockById(id);
  }

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

  default boolean existsByIdAndAnyRole(UUID userId, Collection<String> roles) {
    final var roleInfo = findRoleInfoById(userId);
    return roleInfo
        .map(userRoleInfo -> Arrays.stream(userRoleInfo.getRoles()).anyMatch(roles::contains))
        .orElse(false);
  }

  @Query(
      "SELECT u.id as id, u.name as name, u.roles as roles FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
  Optional<UserRoleInfo> _findRoleInfoById(@Param("id") UUID id);

  /** Surgically retrieves a user's ID, name, and roles. Excludes the system user. */
  default Optional<UserRoleInfo> findRoleInfoById(UUID id) {
    if (id == null || id.equals(CoreUtils.SYSTEM_USER_ID)) {
      return Optional.empty();
    }
    return _findRoleInfoById(id);
  }
}
