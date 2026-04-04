package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.SecretGroupAuthorizationResponseConverter;
import com.example.secrets_manager.api.rest.dto.ModifyAuthorizationRequest;
import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationDetailedResponse;
import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationResponse;
import com.example.secrets_manager.core.models.ModifyAuthorizationPayload;
import com.example.secrets_manager.core.services.SecretGroupAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
  @ApiResponse(
      responseCode = "200",
      description = "Permissions updated successfully",
      content = @Content(schema = @Schema(implementation = SecretGroupAuthorizationResponse.class)))
  @ApiResponse(responseCode = "204", description = "Authorizations fully revoked (entry deleted)")
  @ApiResponse(responseCode = "400", description = "Invalid request or governance violation")
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden: Insufficient privileges or privilege escalation attempt")
  @ApiResponse(responseCode = "404", description = "Group or User not found")
  @PreAuthorize("isAuthenticated()")
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

  @Operation(
      summary = "List all authorizations for a secret group",
      parameters = {
        @Parameter(
            name = "sort",
            in = ParameterIn.QUERY,
            description = "Sorting criteria. Allowed properties: username.",
            schema = @Schema(type = "string"))
      })
  @ApiResponse(
      responseCode = "200",
      description = "List retrieved successfully",
      content = @Content(schema = @Schema(implementation = PagedResponse.class)))
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden: No read access to this group")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public ResponseEntity<PagedResponse<SecretGroupAuthorizationDetailedResponse>> listAuthorizations(
      @PathVariable UUID groupId, @ParameterObject Pageable pageable) {
    var page = authorizationService.listAuthorizations(groupId, pageable);
    var response =
        PagedResponse.fromPage(
            page.map(SecretGroupAuthorizationResponseConverter::fromDetailedModel));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get authorization details for a specific user on a group")
  @ApiResponse(
      responseCode = "200",
      description = "Authorization retrieved successfully",
      content =
          @Content(
              schema = @Schema(implementation = SecretGroupAuthorizationDetailedResponse.class)))
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden: No read access to this group")
  @ApiResponse(responseCode = "404", description = "Authorization not found")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{userId}")
  public ResponseEntity<SecretGroupAuthorizationDetailedResponse> getUserAuthorization(
      @PathVariable UUID groupId, @PathVariable UUID userId) {
    var auth = authorizationService.getUserAuthorization(groupId, userId);
    return ResponseEntity.ok(SecretGroupAuthorizationResponseConverter.fromDetailedModel(auth));
  }
}
