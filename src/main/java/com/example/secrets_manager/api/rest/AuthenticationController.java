package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.core.models.AuthResponse;
import com.example.secrets_manager.core.models.LoginPayload;
import com.example.secrets_manager.core.models.RefreshTokenPayload;
import com.example.secrets_manager.core.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  @Autowired
  public AuthenticationController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Operation(summary = "Authenticate user and issue tokens")
  @ApiResponse(
      responseCode = "200",
      description = "Successfully authenticated",
      content = @Content(schema = @Schema(implementation = AuthResponse.class)))
  @ApiResponse(responseCode = "403", description = "Invalid credentials")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginPayload payload) {
    AuthResponse response = authenticationService.login(payload);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Refresh access token using a valid refresh token")
  @ApiResponse(
      responseCode = "200",
      description = "Successfully refreshed tokens",
      content = @Content(schema = @Schema(implementation = AuthResponse.class)))
  @ApiResponse(responseCode = "401", description = "Invalid or revoked refresh token")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenPayload payload) {
    AuthResponse response = authenticationService.refreshToken(payload);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Log out the current user and invalidate the session")
  @ApiResponse(responseCode = "204", description = "Successfully logged out")
  @ApiResponse(responseCode = "401", description = "Unauthorized: Authentication required")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    authenticationService.logout();
    return ResponseEntity.noContent().build();
  }
}
