package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.UserCreationRequestConverter;
import com.example.secrets_manager.api.rest.converters.UserPasswordUpdateRequestConverter;
import com.example.secrets_manager.api.rest.converters.UserResponseConverter;
import com.example.secrets_manager.api.rest.dto.UserCreationRequest;
import com.example.secrets_manager.api.rest.dto.UserPasswordUpdateRequest;
import com.example.secrets_manager.api.rest.dto.UserResponse;
import com.example.secrets_manager.core.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
