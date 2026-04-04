package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.SecretGroupAuthorizationResponseConverter;
import com.example.secrets_manager.api.rest.dto.ModifyAuthorizationRequest;
import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationResponse;
import com.example.secrets_manager.core.models.ModifyAuthorizationPayload;
import com.example.secrets_manager.core.services.SecretGroupAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/api/v1/secret-groups/{groupId}/authorizations",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
    name = "Secret Group Authorizations",
    description = "Endpoints for managing group-level permissions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class SecretGroupAuthorizationController {

  private final SecretGroupAuthorizationService authorizationService;

  @Autowired
  public SecretGroupAuthorizationController(SecretGroupAuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Operation(summary = "Synchronize a user's permissions for a secret group")
  @ApiResponse(responseCode = "200", description = "Permissions updated successfully")
  @ApiResponse(responseCode = "204", description = "Authorizations fully revoked (entry deleted)")
  @ApiResponse(responseCode = "400", description = "Invalid request or governance violation")
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden: Insufficient privileges or privilege escalation attempt")
  @ApiResponse(responseCode = "404", description = "Group or User not found")
  @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SecretGroupAuthorizationResponse> modifyAuthorization(
      @PathVariable UUID groupId,
      @PathVariable UUID userId,
      @Valid @RequestBody ModifyAuthorizationRequest request) {

    var payload =
        ModifyAuthorizationPayload.builder()
            .groupId(groupId)
            .targetUserId(userId)
            .permissions(request.getPermissions())
            .build();

    var result = authorizationService.modifyAuthorization(payload);

    return result
        .map(auth -> ResponseEntity.ok(SecretGroupAuthorizationResponseConverter.fromModel(auth)))
        .orElse(ResponseEntity.noContent().build());
  }
}
