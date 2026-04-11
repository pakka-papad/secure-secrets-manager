package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.SecretGroupEntityConverter;
import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import com.example.secrets_manager.core.data.repositories.SecretGroupRepository;
import com.example.secrets_manager.core.data.repositories.SecretRepository;
import com.example.secrets_manager.core.models.*;
import com.example.secrets_manager.core.services.exceptions.SecretGroupAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.SecretGroupServiceException;
import com.example.secrets_manager.core.utils.PaginationUtils;
import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Service for managing Secret Groups. */
@Service
@Slf4j
@Validated
public class SecretGroupService {

  private final SecretGroupRepository secretGroupRepository;
  private final SecretRepository secretRepository;
  private final SecretGroupAuthorizationService authorizationService;
  private final CryptographyService cryptographyService;
  private final AuditService auditService;

  @Autowired
  public SecretGroupService(
      SecretGroupRepository secretGroupRepository,
      SecretRepository secretRepository,
      SecretGroupAuthorizationService authorizationService,
      CryptographyService cryptographyService,
      AuditService auditService) {
    this.secretGroupRepository = secretGroupRepository;
    this.secretRepository = secretRepository;
    this.authorizationService = authorizationService;
    this.cryptographyService = cryptographyService;
    this.auditService = auditService;
  }

  /**
   * Creates a new secret group and atomically grants the creator full permissions.
   *
   * @param payload The {@link SecretGroupCreationPayload} containing group details.
   * @return The created {@link SecretGroup} domain model.
   * @throws SecretGroupAlreadyExistsException if an active group with the name exists.
   * @throws IllegalArgumentException if the algorithm is unsupported.
   * @throws AccessDeniedException if the current user lacks the required system roles.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'SECRET_MANAGER')")
  public SecretGroup createGroup(@NotNull @Valid SecretGroupCreationPayload payload) {
    // 1. Validate Algorithm Availability for DATA encryption
    if (!cryptographyService.isAlgorithmSupported(payload.getEncryptAlgo(), CipherPurpose.DATA)) {
      throw new IllegalArgumentException(
          "Unsupported or unauthorized data encryption algorithm: " + payload.getEncryptAlgo());
    }

    // 2. Map to Entity
    final var entity =
        SecretGroupEntity.builder()
            .name(payload.getName())
            .encryptAlgo(payload.getEncryptAlgo())
            .build();

    // 3. Save and Catch
    SecretGroupEntity savedEntity;
    try {
      savedEntity = secretGroupRepository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("uq_sm_secret_groups_active_name")) {
        throw new SecretGroupAlreadyExistsException(
            String.format("Secret group with name '%s' already exists.", payload.getName()), e);
      }
      throw new SecretGroupServiceException(
          "Failed to create group due to data integrity violation.", e);
    }

    // 4. Atomic Authorization: Grant the creator FULL permissions
    final var creatorId = SecurityUtils.getAuthenticatedUserId();
    authorizationService.grantInitialPermissionsInternal(creatorId, savedEntity.getId());

    // 5. Audit
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(creatorId)
            .action(AuditAction.SECRET_GROUP_CREATE)
            .targetGroupId(savedEntity.getId())
            .build());

    return SecretGroupEntityConverter.toModel(savedEntity);
  }

  /**
   * Retrieves a secret group by ID.
   *
   * @param id The UUID of the group to retrieve.
   * @return The {@link SecretGroup} domain model.
   * @throws EntityNotFoundException if the group does not exist or is deleted.
   * @throws AccessDeniedException if the current user is not authorized to read this group.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("@groupAuth.canRead(principal, #id)")
  public SecretGroup getGroup(UUID id) {
    return secretGroupRepository
        .findByIdAndDeletedAtIsNull(id)
        .map(SecretGroupEntityConverter::toModel)
        .orElseThrow(() -> new EntityNotFoundException("Secret group not found: " + id));
  }

  /**
   * Lists all groups authorized for the current user.
   *
   * @param pageable Pagination and sorting information.
   * @return A paginated list of authorized {@link SecretGroup} models.
   * @throws AccessDeniedException if the user is unauthenticated.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<SecretGroup> listGroups(Pageable pageable) {
    PaginationUtils.validateSort(pageable, SecretGroupEntity.ALLOWED_SORT_FIELDS);
    final var userId = SecurityUtils.getAuthenticatedUserId();

    Page<SecretGroupEntity> page;
    if (SecurityUtils.hasRole(UserRole.ADMIN)) {
      page = secretGroupRepository.findAllByDeletedAtIsNull(pageable);
    } else {
      page = secretGroupRepository.findAuthorizedGroups(userId, pageable);
    }

    return page.map(SecretGroupEntityConverter::toModel);
  }

  /**
   * Soft-deletes a secret group.
   *
   * @param id The UUID of the group to delete.
   * @throws EntityNotFoundException if the group does not exist or is deleted.
   * @throws IllegalStateException if the group still contains active secrets.
   * @throws AccessDeniedException if the current user is not authorized to delete this group.
   */
  @Transactional
  @PreAuthorize("@groupAuth.canDelete(principal, #id)")
  public void deleteGroup(UUID id) {
    // 1. Find and validate existence
    var entity =
        secretGroupRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new EntityNotFoundException("Secret group not found: " + id));

    // 2. Prevent deletion if active secrets exist
    long secretCount = secretRepository.countByGroupIdAndDeletedAtIsNull(id);
    if (secretCount > 0) {
      throw new IllegalStateException("Cannot delete group: It still contains active secrets.");
    }

    // 3. Perform Soft Delete
    entity.setDeletedAt(Instant.now());
    secretGroupRepository.save(entity);

    // 4. Audit
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(SecurityUtils.getAuthenticatedUserId())
            .action(AuditAction.SECRET_GROUP_DELETE)
            .targetGroupId(id)
            .build());
  }
}
