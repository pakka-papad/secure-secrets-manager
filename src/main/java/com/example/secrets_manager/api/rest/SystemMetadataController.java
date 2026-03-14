package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.SymmetricAlgorithmMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for exposing system capabilities and metadata. */
@RestController
@RequestMapping(value = "/api/v1/system", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "System Metadata", description = "Endpoints for discovering system capabilities")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class SystemMetadataController {

  private final CryptographyService cryptographyService;

  @Autowired
  public SystemMetadataController(CryptographyService cryptographyService) {
    this.cryptographyService = cryptographyService;
  }

  @Operation(summary = "List all supported symmetric encryption algorithms with metadata")
  @ApiResponse(responseCode = "200", description = "List retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @GetMapping("/algorithms/symmetric")
  public ResponseEntity<List<SymmetricAlgorithmMetadata>> getSymmetricAlgorithms() {
    return ResponseEntity.ok(cryptographyService.getSupportedSymmetricAlgorithms());
  }
}
