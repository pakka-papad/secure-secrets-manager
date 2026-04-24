package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecretEntity;
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

  /** Finds all active secrets in a group with pre-fetched metadata. */
  @EntityGraph(attributePaths = {"group", "masterKey"})
  List<SecretEntity> findAllByGroupIdAndDeletedAtIsNull(UUID groupId);

  /** Finds a specific secret by name within a group. */
  @EntityGraph(attributePaths = {"group", "masterKey"})
  Optional<SecretEntity> findByGroupIdAndSecretNameAndDeletedAtIsNull(
      UUID groupId, String secretName);

  long countByGroupIdAndDeletedAtIsNull(UUID groupId);

  /** Overrides standard findAll to ensure associated metadata is pre-fetched during searches. */
  @Override
  @EntityGraph(attributePaths = {"group", "masterKey"})
  @NonNull Page<SecretEntity> findAll(
      @NonNull Specification<SecretEntity> spec, @NonNull Pageable pageable);
}
