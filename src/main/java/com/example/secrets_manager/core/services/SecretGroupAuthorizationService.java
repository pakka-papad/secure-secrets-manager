package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.CacheConstants;
import com.example.secrets_manager.core.data.converters.SecretGroupAuthorizationEntityConverter;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationRepository;
import com.example.secrets_manager.core.data.repositories.SecretGroupRepository;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.*;
import com.example.secrets_manager.core.utils.PaginationUtils;
import com.example.secrets_manager.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Service for managing user authorizations on secret groups. Enforces a strict governance model
 * where actors can only grant or revoke permissions they themselves possess.
 */
@Service
@Slf4j
@Validated
public class SecretGroupAuthorizationService {

  private final SecretGroupAuthorizationRepository authorizationRepository;
  private final SecretGroupRepository secretGroupRepository;
  private final UserRepository userRepository;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  @Autowired
  public SecretGroupAuthorizationService(
      SecretGroupAuthorizationRepository authorizationRepository,
      SecretGroupRepository secretGroupRepository,
      UserRepository userRepository,
      AuditService auditService,
      ObjectMapper objectMapper) {
    this.authorizationRepository = authorizationRepository;
    this.secretGroupRepository = secretGroupRepository;
    this.userRepository = userRepository;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  /**
   * Synchronizes the user's permissions on a secret group to match the desired state. Enforces role
   * requirements and the Mirroring Principle.
   *
   * @param payload The {@link ModifyAuthorizationPayload} containing the target state.
   * @return An {@link Optional} containing the updated {@link SecretGroupAuthorization} if it
   *     exists, or empty if permissions were fully revoked.
   * @throws AccessDeniedException if the actor lacks sufficient privileges or attempts privilege
   *     escalation.
   */
  @Transactional
  @CacheEvict(
      value = CacheConstants.CACHE_SECRET_GROUP_AUTHORIZATIONS,
      key = "#payload.targetUserId.toString() + '-' + #payload.groupId")
  public Optional<SecretGroupAuthorization> modifyAuthorization(
      @NotNull @Valid ModifyAuthorizationPayload payload) throws AccessDeniedException {
    final var actorId = SecurityUtils.getAuthenticatedUserId();
    final var targetId = payload.getTargetUserId();
    final var groupId = payload.getGroupId();

    validateGroupActive(groupId);
    validateUserActive(targetId);

    // 1. Get both Actor and Target authorizations at once
    final var auths =
        authorizationRepository.findAllByIdGroupIdAndIdUserIdIn(
            groupId, List.of(actorId, targetId));

    final var actorAuthEntity =
        auths.stream().filter(e -> e.getId().getUserId().equals(actorId)).findFirst().orElse(null);

    var targetAuthEntity =
        auths.stream().filter(e -> e.getId().getUserId().equals(targetId)).findFirst().orElse(null);

    // 2. Identify Changes (Added and Removed)
    final var currentTargetPermissions =
        (targetAuthEntity == null)
            ? EnumSet.noneOf(PermissionType.class)
            : toPermissionSet(targetAuthEntity);

    final var desiredPermissions = payload.getPermissions();
    final var added =
        desiredPermissions.stream()
            .filter(p -> !currentTargetPermissions.contains(p))
            .collect(Collectors.toSet());
    final var removed =
        currentTargetPermissions.stream()
            .filter(p -> !desiredPermissions.contains(p))
            .collect(Collectors.toSet());

    if (added.isEmpty() && removed.isEmpty() && targetAuthEntity != null) {
      return Optional.of(SecretGroupAuthorizationEntityConverter.toModel(targetAuthEntity));
    }

    // 3. Validate Actor authority
    validateActorAuthority(actorAuthEntity, added, removed);

    // 4. Handle Total Revocation (Delete the row)
    var targetAuthId = new SecretGroupAuthorizationId(targetId, groupId);
    if (desiredPermissions.isEmpty()) {
      authorizationRepository.deleteById(targetAuthId);
      log.info(
          "Purged authorization record for user {} on group {} (Total Revocation).",
          targetId,
          groupId);

      createAuditLog(actorId, targetId, groupId, desiredPermissions);
      return Optional.empty();
    }
    // 5. Governance Guardrail: Only SECRET_MANAGERs can receive DELETE permission
    if (added.contains(PermissionType.DELETE)) {
      validateTargetUserForDeletePermission(targetId);
    }

    // 6. Apply Sync (Update or Insert)
    if (targetAuthEntity == null) {
      targetAuthEntity = SecretGroupAuthorizationEntity.builder().id(targetAuthId).build();
    }
    targetAuthEntity.setPRead(desiredPermissions.contains(PermissionType.READ));
    targetAuthEntity.setPWrite(desiredPermissions.contains(PermissionType.WRITE));
    targetAuthEntity.setPDelete(desiredPermissions.contains(PermissionType.DELETE));
    targetAuthEntity.setModifiedAt(Instant.now());

    var savedEntity = authorizationRepository.save(targetAuthEntity);

    // 7. Create audit log entry
    createAuditLog(actorId, targetId, groupId, desiredPermissions);

    return Optional.of(SecretGroupAuthorizationEntityConverter.toModel(savedEntity));
  }

  /** Retrieves a paged list of authorizations for a group. */
  @Transactional(readOnly = true)
  @PreAuthorize("@groupAuth.canRead(principal, #groupId)")
  public Page<SecretGroupAuthorizationDetailed> listAuthorizations(
      UUID groupId, Pageable pageable) {
    validateGroupActive(groupId);
    PaginationUtils.validateSort(pageable, SecretGroupAuthorizationEntity.ALLOWED_SORT_FIELDS);
    return authorizationRepository
        .findAllByGroupIdSurgical(groupId, pageable)
        .map(info -> SecretGroupAuthorizationEntityConverter.toDetailedModel(info, groupId));
  }

  /** Retrieves authorization details for a specific user on a group. */
  @Transactional(readOnly = true)
  @PreAuthorize("@groupAuth.canRead(principal, #groupId)")
  public SecretGroupAuthorizationDetailed getUserAuthorization(UUID groupId, UUID userId) {
    validateGroupActive(groupId);
    return authorizationRepository
        .findByGroupIdAndUserIdSurgical(groupId, userId)
        .map(info -> SecretGroupAuthorizationEntityConverter.toDetailedModel(info, groupId))
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    String.format(
                        "Authorization not found for user %s in group %s", userId, groupId)));
  }

  private void validateGroupActive(UUID groupId) {
    if (!secretGroupRepository.existsByIdAndDeletedAtIsNull(groupId)) {
      throw new EntityNotFoundException(
          String.format("Secret Group not found or deleted: %s", groupId));
    }
  }

  private void validateUserActive(UUID userId) {
    if (!userRepository.existsByIdAndAnyRole(userId, List.of(UserRole.USER.name()))) {
      throw new EntityNotFoundException(String.format("User not found or deleted: %s", userId));
    }
  }

  private void createAuditLog(
      UUID actorId, UUID targetId, UUID groupId, Set<PermissionType> desiredPermissions) {
    final var detailsMap =
        Map.of("new_permissions", desiredPermissions.stream().map(Enum::name).toList());
    String details = null;
    try {
      details = objectMapper.writeValueAsString(detailsMap);
    } catch (JsonProcessingException jpe) {
      log.error("Cannot serialize secret group authorization details", jpe);
    }
    var auditPayload =
        AuditLogPayload.builder()
            .actorUserId(actorId)
            .action(AuditAction.AUTHORIZATION_UPDATE)
            .targetUserId(targetId)
            .targetGroupId(groupId)
            .details(details)
            .build();
    auditService.save(auditPayload);
  }

  /** Internal method to grant full permissions to a user upon group creation. */
  @Transactional
  public SecretGroupAuthorization grantInitialPermissionsInternal(UUID userId, UUID groupId) {
    var id = new SecretGroupAuthorizationId(userId, groupId);
    var entity =
        SecretGroupAuthorizationEntity.builder()
            .id(id)
            .pRead(true)
            .pWrite(true)
            .pDelete(true)
            .modifiedAt(Instant.now())
            .build();

    var saved = authorizationRepository.save(entity);
    log.info("Granted initial FULL permissions to user {} for new group {}.", userId, groupId);
    return SecretGroupAuthorizationEntityConverter.toModel(saved);
  }

  private void validateActorAuthority(
      SecretGroupAuthorizationEntity actorAuthEntity,
      Set<PermissionType> added,
      Set<PermissionType> removed) {
    // a) Administrators have global management rights
    if (SecurityUtils.hasRole(UserRole.ADMIN)) {
      return;
    }

    // b) Only SECRET_MANAGERs can manage group authorizations
    if (!SecurityUtils.hasRole(UserRole.SECRET_MANAGER)) {
      throw new AccessDeniedException(
          "Insufficient role: Only Administrators or Managers can modify authorizations.");
    }

    // c) Actor must possess every permission they try to grant or revoke
    if (actorAuthEntity == null) {
      throw new AccessDeniedException(
          "Permission Denied: You have no permissions on this group and cannot manage its ACL.");
    }

    final var actorPermissions = toPermissionSet(actorAuthEntity);

    // Actors MUST have WRITE permission to manage any part of the ACL
    if (!actorPermissions.contains(PermissionType.WRITE)) {
      throw new AccessDeniedException(
          "Permission Denied: You require WRITE access to manage a group's authorizations.");
    }

    // Check additions
    for (PermissionType p : added) {
      if (!actorPermissions.contains(p)) {
        throw new AccessDeniedException(
            String.format(
                "Privilege Escalation Blocked: You cannot grant the %s permission because you do not possess it yourself.",
                p));
      }
    }

    // Check removals
    for (PermissionType p : removed) {
      if (!actorPermissions.contains(p)) {
        throw new AccessDeniedException(
            String.format(
                "Sabotage Blocked: You cannot revoke the %s permission because you do not possess it yourself.",
                p));
      }
    }
  }

  private void validateTargetUserForDeletePermission(UUID targetUserId) {
    boolean isAuthorizedRole =
        userRepository.existsByIdAndAnyRole(
            targetUserId, List.of(UserRole.SECRET_MANAGER.name(), UserRole.ADMIN.name()));

    if (!isAuthorizedRole) {
      throw new AccessDeniedException(
          "Governance Violation: Only users with the SECRET_MANAGER or ADMIN role can be granted DELETE permission.");
    }
  }

  private Set<PermissionType> toPermissionSet(SecretGroupAuthorizationEntity entity) {
    Set<PermissionType> set = EnumSet.noneOf(PermissionType.class);
    if (entity.isPRead()) set.add(PermissionType.READ);
    if (entity.isPWrite()) set.add(PermissionType.WRITE);
    if (entity.isPDelete()) set.add(PermissionType.DELETE);
    return set;
  }
}
