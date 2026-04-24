package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.SecretMetadataResponseConverter;
import com.example.secrets_manager.api.rest.dto.*;
import com.example.secrets_manager.core.models.SecretCreationPayload;
import com.example.secrets_manager.core.models.SecretValueUpdatePayload;
import com.example.secrets_manager.core.models.search.SecretSearchCriteria;
import com.example.secrets_manager.core.services.SecretService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/secret-groups/{groupId}/secrets")
@Tag(name = "Secrets", description = "Endpoints for managing secrets within groups")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SecretController {

  private final SecretService secretService;

  @Operation(summary = "Create a new secret in a group")
  @ApiResponse(responseCode = "201", description = "Secret created successfully")
  @PostMapping
  public ResponseEntity<SecretMetadataResponse> createSecret(
      @PathVariable UUID groupId, @Valid @RequestBody CreateSecretRequest request) {

    final var payload =
        SecretCreationPayload.builder()
            .name(request.getName())
            .plaintextValue(request.getPlaintextValue())
            .build();

    final var secret = secretService.createSecret(groupId, payload);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SecretMetadataResponseConverter.fromModel(secret));
  }

  @Operation(
      summary = "List all secrets in a group with pagination and filters (metadata only)",
      parameters = {
        @Parameter(
            name = "sort",
            in = ParameterIn.QUERY,
            description =
                "Sorting criteria in the format: property,(asc|desc). Allowed properties: secretName.",
            schema = @Schema(type = "string")),
        @Parameter(
            name = "namePrefix",
            in = ParameterIn.QUERY,
            description = "Filter secrets whose names start with this prefix.",
            schema = @Schema(type = "string"))
      })
  @ApiResponse(responseCode = "200", description = "List retrieved successfully")
  @GetMapping
  public ResponseEntity<PagedResponse<SecretMetadataResponse>> listSecrets(
      @PathVariable UUID groupId,
      @ParameterObject SecretSearchCriteria criteria,
      @ParameterObject Pageable pageable) {
    final var page = secretService.listSecrets(groupId, criteria, pageable);
    final var response =
        PagedResponse.fromPage(page.map(SecretMetadataResponseConverter::fromModel));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get secret metadata")
  @GetMapping("/{name}")
  public ResponseEntity<SecretMetadataResponse> getSecretMetadata(
      @PathVariable UUID groupId, @PathVariable String name) {
    final var secret = secretService.getSecretMetadata(groupId, name);
    return ResponseEntity.ok(SecretMetadataResponseConverter.fromModel(secret));
  }

  @Operation(summary = "Reveal secret plaintext value")
  @GetMapping("/{name}/value")
  public ResponseEntity<SecretValueResponse> getSecretValue(
      @PathVariable UUID groupId, @PathVariable String name) {
    final var plaintext = secretService.getSecretValue(groupId, name);
    return ResponseEntity.ok(
        SecretValueResponse.builder().name(name).plaintextValue(plaintext).build());
  }

  @Operation(summary = "Update a secret's value")
  @PutMapping("/{name}/value")
  public ResponseEntity<SecretMetadataResponse> updateSecretValue(
      @PathVariable UUID groupId,
      @PathVariable String name,
      @Valid @RequestBody UpdateSecretValueRequest request) {

    final var payload = new SecretValueUpdatePayload(request.getPlaintextValue());
    final var secret = secretService.updateSecretValue(groupId, name, payload);
    return ResponseEntity.ok(SecretMetadataResponseConverter.fromModel(secret));
  }

  @Operation(summary = "Delete a secret")
  @DeleteMapping("/{name}")
  public ResponseEntity<Void> deleteSecret(@PathVariable UUID groupId, @PathVariable String name) {
    secretService.deleteSecret(groupId, name);
    return ResponseEntity.noContent().build();
  }
}
