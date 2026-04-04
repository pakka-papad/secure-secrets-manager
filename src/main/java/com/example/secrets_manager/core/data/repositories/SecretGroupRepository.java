package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SecretGroupRepository extends JpaRepository<SecretGroupEntity, UUID> {

  Optional<SecretGroupEntity> findByIdAndDeletedAtIsNull(UUID id);

  boolean existsByIdAndDeletedAtIsNull(UUID id);

  Optional<SecretGroupEntity> findByNameAndDeletedAtIsNull(String name);

  /**
   * Finds all groups that a specific user is authorized to see. Optimized using an EXISTS semi-join
   * to prevent duplicate rows and minimize overhead.
   */
  @Query(
      """
      SELECT g FROM SecretGroupEntity g
      WHERE g.deletedAt IS NULL
      AND EXISTS (
          SELECT 1 FROM SecretGroupAuthorizationEntity a
          WHERE a.id.groupId = g.id
          AND a.id.userId = :userId
      )
      """)
  Page<SecretGroupEntity> findAuthorizedGroups(UUID userId, Pageable pageable);

  /** Administrators can see all non-deleted groups. */
  Page<SecretGroupEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
