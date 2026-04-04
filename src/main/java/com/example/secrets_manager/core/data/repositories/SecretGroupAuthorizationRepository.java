package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SecretGroupAuthorizationRepository
    extends JpaRepository<SecretGroupAuthorizationEntity, SecretGroupAuthorizationId> {

  /**
   * Deletes all secret group authorizations for a specific user.
   *
   * @param userId The ID of the user whose authorizations should be removed.
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM SecretGroupAuthorizationEntity a WHERE a.id.userId = :userId")
  void deleteByIdUserId(UUID userId);

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

  /**
   * Finds multiple authorization records for a specific group.
   *
   * @param groupId The ID of the secret group.
   * @param userIds The collection of user IDs to fetch.
   * @return A list of authorization entities.
   */
  List<SecretGroupAuthorizationEntity> findAllByIdGroupIdAndIdUserIdIn(
      UUID groupId, Collection<UUID> userIds);

  /**
   * Surgically revokes DELETE permissions for a specific user across all groups.
   *
   * @param userId THe ID of the user whose delete permissions are to be revoked.
   */
  @Modifying
  @Query(
      "UPDATE SecretGroupAuthorizationEntity a SET a.pDelete = false WHERE a.id.userId = :userId")
  void revokeDeletePermissionForUser(UUID userId);

  /** Surgically retrieves a paged list of authorizations for a group, joined with usernames. */
  @Query(
      "SELECT a.id.userId as userId, u.name as username, "
          + "a.pRead as PRead, a.pWrite as PWrite, a.pDelete as PDelete, a.modifiedAt as modifiedAt "
          + "FROM SecretGroupAuthorizationEntity a "
          + "JOIN UserEntity u ON a.id.userId = u.id "
          + "WHERE a.id.groupId = :groupId AND u.deletedAt IS NULL")
  Page<SecretGroupAuthorizationInfo> findAllByGroupIdSurgical(
      @Param("groupId") UUID groupId, Pageable pageable);

  /** Surgically retrieves authorization details for a specific user on a specific group. */
  @Query(
      "SELECT a.id.userId as userId, u.name as username, "
          + "a.pRead as PRead, a.pWrite as PWrite, a.pDelete as PDelete, a.modifiedAt as modifiedAt "
          + "FROM SecretGroupAuthorizationEntity a "
          + "JOIN UserEntity u ON a.id.userId = u.id "
          + "WHERE a.id.groupId = :groupId AND a.id.userId = :userId AND u.deletedAt IS NULL")
  Optional<SecretGroupAuthorizationInfo> findByGroupIdAndUserIdSurgical(
      @Param("groupId") UUID groupId, @Param("userId") UUID userId);
}
