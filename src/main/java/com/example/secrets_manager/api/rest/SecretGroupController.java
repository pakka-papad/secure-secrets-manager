package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.SecretGroupResponseConverter;
import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.SecretGroupCreationRequest;
import com.example.secrets_manager.api.rest.dto.SecretGroupResponse;
import com.example.secrets_manager.core.services.SecretGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/secret-groups", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Secret Groups", description = "Secret group management APIs")
@SecurityRequirement(name = "bearerAuth")
public class SecretGroupController {

  private final SecretGroupService secretGroupService;

  @Autowired
  public SecretGroupController(SecretGroupService secretGroupService) {
    this.secretGroupService = secretGroupService;
  }

  @Operation(summary = "Create a new secret group")
  @ApiResponse(responseCode = "201", description = "Group created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid algorithm or name collision")
  @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient role")
  @PreAuthorize("hasAnyRole('ADMIN', 'SECRET_MANAGER')")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SecretGroupResponse> createGroup(
      @Valid @RequestBody SecretGroupCreationRequest request) {
    var payload =
        com.example.secrets_manager.api.rest.converters.SecretGroupCreationRequestConverter.toModel(
            request);
    var group = secretGroupService.createGroup(payload);
    return new ResponseEntity<>(SecretGroupResponseConverter.fromModel(group), HttpStatus.CREATED);
  }

  @Operation(summary = "Get a secret group by ID")
  @ApiResponse(responseCode = "200", description = "Group retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden: No read access to this group")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @GetMapping("/{id}")
  public ResponseEntity<SecretGroupResponse> getGroup(@PathVariable UUID id) {
    var group = secretGroupService.getGroup(id);
    return ResponseEntity.ok(SecretGroupResponseConverter.fromModel(group));
  }

  @Operation(
      summary = "List all secret groups authorized for the current user",
      parameters = {
        @Parameter(
            name = "sort",
            in = ParameterIn.QUERY,
            description =
                "Sorting criteria in the format: property,(asc|desc). Allowed properties: name.",
            schema = @Schema(type = "string"))
      })
  @ApiResponse(responseCode = "200", description = "List retrieved successfully")
  @GetMapping
  public ResponseEntity<PagedResponse<SecretGroupResponse>> listGroups(
      @ParameterObject Pageable pageable) {
    var page = secretGroupService.listGroups(pageable);
    var response = PagedResponse.fromPage(page.map(SecretGroupResponseConverter::fromModel));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Delete a secret group")
  @ApiResponse(responseCode = "204", description = "Group deleted successfully")
  @ApiResponse(responseCode = "400", description = "Bad Request: Contains active secrets")
  @ApiResponse(responseCode = "403", description = "Forbidden: No delete access")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
    secretGroupService.deleteGroup(id);
    return ResponseEntity.noContent().build();
  }
}
