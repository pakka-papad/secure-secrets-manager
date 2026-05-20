package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.services.MasterKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API Controller for administrative Master Key management. */
@RestController
@RequestMapping(value = "/api/v1/admin/master-keys", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin: Master Keys", description = "Administrative Master Key management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminMasterKeyController {

  private final MasterKeyService masterKeyService;

  @Operation(summary = "Mark a master key as compromised")
  @ApiResponse(responseCode = "200", description = "Key successfully marked as compromised")
  @ApiResponse(responseCode = "403", description = "Forbidden: Admin role required")
  @ApiResponse(responseCode = "404", description = "Master key not found")
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/{version}/compromise")
  public ResponseEntity<MasterKey> markKeyAsCompromised(@PathVariable int version) {
    MasterKey updatedKey = masterKeyService.markKeyAsCompromised(version);
    return ResponseEntity.ok(updatedKey);
  }
}
