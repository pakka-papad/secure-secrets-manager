package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.AuthorizationEntityConverter;
import com.example.secrets_manager.core.data.entities.AuthorizationEntity;
import com.example.secrets_manager.core.data.entities.AuthorizationId;
import com.example.secrets_manager.core.data.repositories.AuthorizationRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.Authorization;
import com.example.secrets_manager.core.models.ModifyAuthorizationPayload;
import com.example.secrets_manager.core.models.PermissionType;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationService {

  private final AuthorizationRepository authorizationRepository;
  private final AuditService auditService;

  @Autowired
  public AuthorizationService(
      AuthorizationRepository authorizationRepository, AuditService auditService) {
    this.authorizationRepository = authorizationRepository;
    this.auditService = auditService;
  }

  /**
   * Grants or revokes a permission for a user on a specific secret group.
   *
   * @param payload The {@link ModifyAuthorizationPayload} containing all details for the operation.
   * @throws AccessDeniedException if the actor does not have sufficient permission to perform the
   *     action.
   */
  @Transactional
  public void modifyAuthorization(ModifyAuthorizationPayload payload) throws AccessDeniedException {
    // 1. Get and lock the authorization for the user PERFORMING the action (the actor)
    var actorAuthId =
        AuthorizationId.builder()
            .userId(payload.getActorUserId())
            .groupId(payload.getGroupId())
            .build();
    var actorAuth =
        authorizationRepository
            .findAndLockById(actorAuthId) // Use the locking method to prevent race conditions
            .map(AuthorizationEntityConverter::toModel)
            .orElse(null); // Actor might have no permissions

    // 2. Check if the actor is allowed to perform the action
    boolean isAllowed;
    if (payload.isGrant()) {
      isAllowed = canGrant(actorAuth, payload.getPermission());
    } else {
      isAllowed = canRevoke(actorAuth, payload.getPermission());
    }

    if (!isAllowed) {
      throw new AccessDeniedException(
          String.format(
              "User %s is not authorized to %s permission %s on group %s",
              payload.getActorUserId(),
              payload.isGrant() ? "grant" : "revoke",
              payload.getPermission(),
              payload.getGroupId()));
    }

    // 3. Get or create the authorization for the TARGET user
    var targetAuthId =
        AuthorizationId.builder()
            .userId(payload.getTargetUserId())
            .groupId(payload.getGroupId())
            .build();
    var targetAuthEntity =
        authorizationRepository
            .findById(targetAuthId)
            .orElseGet(
                () ->
                    AuthorizationEntity.builder()
                        .id(targetAuthId)
                        .pRead(false)
                        .pWrite(false)
                        .pDelete(false)
                        .build());

    // 4. Modify the target's permissions
    switch (payload.getPermission()) {
      case READ -> targetAuthEntity.setPRead(payload.isGrant());
      case WRITE -> targetAuthEntity.setPWrite(payload.isGrant());
      case DELETE -> targetAuthEntity.setPDelete(payload.isGrant());
    }
    targetAuthEntity.setModifiedAt(Instant.now());

    authorizationRepository.save(targetAuthEntity);

    // 5. Create audit log entry
    var auditPayload =
        AuditLogPayload.builder()
            .actorUserId(payload.getActorUserId())
            .action(
                payload.isGrant()
                    ? AuditAction.AUTHORIZATION_GRANT
                    : AuditAction.AUTHORIZATION_REVOKE)
            .targetUserId(payload.getTargetUserId())
            .targetGroupId(payload.getGroupId())
            .details(String.format("{\"permission\":\"%s\"}", payload.getPermission().name()))
            .build();
    auditService.save(auditPayload);
  }

  /**
   * Checks if a user has the required permissions to grant a specific permission to another user.
   *
   * @param actorAuth The Authorization object of the user performing the grant.
   * @param permissionToGrant The permission being granted.
   * @return true if the grant is allowed, false otherwise.
   */
  private boolean canGrant(Authorization actorAuth, PermissionType permissionToGrant) {
    if (actorAuth == null) {
      return false;
    }

    return switch (permissionToGrant) {
      case READ, WRITE ->
          // Rule: Any user with r & w permission can grant r & w.
          actorAuth.isPRead() && actorAuth.isPWrite();
      case DELETE ->
          // Rule: A user with r, w & d can grant d permission.
          actorAuth.isPRead() && actorAuth.isPWrite() && actorAuth.isPDelete();
      default -> false;
    };
  }

  /**
   * Checks if a user has the required permissions to revoke a specific permission from another
   * user.
   *
   * @param actorAuth The Authorization object of the user performing the revocation.
   * @param permissionToRevoke The permission being revoked.
   * @return true if the revocation is allowed, false otherwise.
   */
  private boolean canRevoke(Authorization actorAuth, PermissionType permissionToRevoke) {
    // For this design, the logic to revoke is the same as the logic to grant.
    return canGrant(actorAuth, permissionToRevoke);
  }
}
