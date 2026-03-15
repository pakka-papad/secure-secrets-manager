package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.UserCreationRequestConverter;
import com.example.secrets_manager.api.rest.converters.UserPasswordUpdateRequestConverter;
import com.example.secrets_manager.api.rest.converters.UserResponseConverter;
import com.example.secrets_manager.api.rest.dto.*;
import com.example.secrets_manager.core.services.UserService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(
      summary = "List users with filters and pagination",
      parameters = {
        @Parameter(
            name = "sort",
            in = ParameterIn.QUERY,
            description =
                "Sorting criteria in the format: property,(asc|desc). Only a single sort dimension is allowed. Allowed properties: name.",
            schema = @Schema(type = "string"))
      })
  @ApiResponse(
      responseCode = "200",
      description = "List retrieved successfully",
      content = @Content(schema = @Schema(implementation = PagedResponse.class)))
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden: Admin role required")
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<PagedResponse<UserResponse>> listUsers(
      @ParameterObject UserSearchCriteria criteria, @ParameterObject Pageable pageable) {
    var page = userService.listUsers(criteria, pageable);
    var response = PagedResponse.fromPage(page.map(UserResponseConverter::fromModel));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get current user profile")
  @ApiResponse(
      responseCode = "200",
      description = "Profile retrieved successfully",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "User not found")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser() {
    var user = userService.getCurrentUser();
    return ResponseEntity.ok(UserResponseConverter.fromModel(user));
  }

  @Operation(summary = "Create a new user")
  @ApiResponse(
      responseCode = "201",
      description = "User created successfully",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid input or password policies violated")
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized: Only authenticated users can create new users")
  @ApiResponse(responseCode = "403", description = "Forbidden: Admin role required")
  @ApiResponse(responseCode = "409", description = "User with given name already exists")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreationRequest request) {
    var user = userService.createUser(UserCreationRequestConverter.toModel(request));
    return new ResponseEntity<>(UserResponseConverter.fromModel(user), HttpStatus.CREATED);
  }

  @Operation(summary = "Update user password")
  @ApiResponse(
      responseCode = "200",
      description = "Password updated successfully",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @ApiResponse(responseCode = "401", description = "Unauthorized: Authentication required")
  @ApiResponse(responseCode = "403", description = "Forbidden: Incorrect old password")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  @PreAuthorize("isAuthenticated()")
  @PutMapping(value = "/me/password", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserResponse> updatePassword(
      @Valid @RequestBody UserPasswordUpdateRequest request) {
    var user = userService.updatePassword(UserPasswordUpdateRequestConverter.toModel(request));
    return ResponseEntity.ok(UserResponseConverter.fromModel(user));
  }

  @Operation(summary = "Update user roles")
  @ApiResponse(
      responseCode = "200",
      description = "Roles updated successfully",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid input or system deadlock prevented")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden: Admin role required or self-demotion attempted")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "409", description = "Conflict: Demotion of last admin attempted")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping(value = "/{userId}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserResponse> updateRoles(
      @PathVariable UUID userId, @Valid @RequestBody UserRolesUpdateRequest request) {
    var user = userService.updateRoles(userId, request.getRoles());
    return ResponseEntity.ok(UserResponseConverter.fromModel(user));
  }

  @Operation(summary = "Delete a user")
  @ApiResponse(responseCode = "204", description = "User deleted successfully")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden: Admin role required or self-deletion attempted")
  @ApiResponse(responseCode = "404", description = "User not found")
  @ApiResponse(responseCode = "409", description = "Conflict: Deletion of last admin attempted")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping(value = "/{userId}")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
    userService.deleteUser(userId);
    return ResponseEntity.noContent().build();
  }
}
