package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecretEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SecretRepository
    extends JpaRepository<SecretEntity, UUID>, JpaSpecificationExecutor<SecretEntity> {

  /**
   * Finds a secret by ID and pre-fetches the associated group and master key records. This avoids
   * the N+1 problem and ensures algorithm metadata is available.
   */
  @EntityGraph(attributePaths = {"group", "masterKey"})
  Optional<SecretEntity> findByIdAndDeletedAtIsNull(UUID id);

  /** Finds a secret by ID and acquires a pessimistic write lock. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
  @EntityGraph(attributePaths = {"group", "masterKey"})
  Optional<SecretEntity> findAndLockByIdAndDeletedAtIsNull(UUID id);

  /** Finds all active secrets in a group with pre-fetched metadata. */
  @EntityGraph(attributePaths = {"group", "masterKey"})
  List<SecretEntity> findAllByGroupIdAndDeletedAtIsNull(UUID groupId);

  /** Finds a specific secret by name within a group. */
  @EntityGraph(attributePaths = {"group", "masterKey"})
  Optional<SecretEntity> findByGroupIdAndSecretNameAndDeletedAtIsNull(
      UUID groupId, String secretName);

  /** Finds a secret by name and acquires a pessimistic write lock to prevent concurrent updates. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
  @EntityGraph(attributePaths = {"group", "masterKey"})
  Optional<SecretEntity> findAndLockByGroupIdAndSecretNameAndDeletedAtIsNull(
      UUID groupId, String secretName);

  long countByGroupIdAndDeletedAtIsNull(UUID groupId);

  /**
   * Finds IDs of active secrets that are protected by a master key version older than the specified
   * target version. Supports keyset pagination via lastId for robust batch processing.
   */
  @Query(
      """
      SELECT s.id FROM SecretEntity s
      WHERE s.masterKeyVersion < :version
      AND (:lastId IS NULL OR s.id > :lastId)
      AND s.deletedAt IS NULL
      ORDER BY s.id ASC
      """)
  List<UUID> findSecretIdsByMasterKeyVersionLessThan(
      @Param("version") int version, @Param("lastId") UUID lastId, Pageable pageable);

  /**
   * Performs an atomic re-wrap update of a secret's DEK, strictly fenced by task assignment. The
   * update only succeeds if the specified worker still owns the task.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
      UPDATE sm.secrets s
      SET dek_ciphertext = :dek, dek_nonce = :nonce, dek_auth_tag = :tag,
          master_key_version = :mkVersion, modified_at = clock_timestamp()
      WHERE s.id = :secretId
      AND EXISTS (
          SELECT 1 FROM sm.task_assignments ta
          JOIN sm.tasks t ON ta.task_id = t.id
          WHERE ta.task_id = :taskId AND ta.worker_id = :workerId
          AND t.state != 'CANCELLED'
      )
      """,
      nativeQuery = true)
  int updateSecretFenced(
      @Param("secretId") UUID secretId,
      @Param("taskId") UUID taskId,
      @Param("workerId") UUID workerId,
      @Param("dek") byte[] dek,
      @Param("nonce") byte[] nonce,
      @Param("tag") byte[] tag,
      @Param("mkVersion") int mkVersion);

  /**
   * Checks if any active secrets exist that are protected by a master key version older than the
   * specified target. Uses short-circuiting EXISTS logic.
   */
  boolean existsByMasterKeyVersionLessThanAndDeletedAtIsNull(int version);

  /** Overrides standard findAll to ensure associated metadata is pre-fetched during searches. */
  @Override
  @EntityGraph(attributePaths = {"group", "masterKey"})
  @NonNull Page<SecretEntity> findAll(
      @NonNull Specification<SecretEntity> spec, @NonNull Pageable pageable);
}
