package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.core.components.MasterKeyProvider;
import com.example.secrets_manager.core.services.InternalMasterKeyService;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEST-ONLY Controller for triggering system events during E2E testing. This class is strictly
 * restricted to the 'test' profile and will not load in production.
 */
@RestController
@RequestMapping("/api/v1/admin/test")
@Tag(name = "Admin: Test Utilities", description = "Endpoints for triggering E2E test scenarios")
@RequiredArgsConstructor
@Profile("test")
@Slf4j
public class AdminTaskTestController {

  private final InternalMasterKeyService internalMasterKeyService;
  private final MasterKeyProvider masterKeyProvider;
  private final CryptographyService cryptographyService;
  private final SecureRandom secureRandom = new SecureRandom();

  @Operation(summary = "Triggers a Master Key promotion event (Simulates discovery of a new key)")
  @ApiResponse(responseCode = "202", description = "Promotion triggered")
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/promote-master-key/{version}")
  public ResponseEntity<Void> triggerPromotion(@PathVariable int version) {
    log.info("E2E Test: Manually triggering promotion for Master Key v{}", version);

    // Generate a valid key for the new version
    String defaultAlgo = "AES-256-GCM";
    int keySize = cryptographyService.getRequiredSymmetricKeySizeBytes(defaultAlgo);
    byte[] newKeyBytes = new byte[keySize];
    secureRandom.nextBytes(newKeyBytes);

    // Use standard Java reflection to populate the private map in
    try {
      Field field = MasterKeyProvider.class.getDeclaredField("masterKeys");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<Integer, byte[]> masterKeysMap = (Map<Integer, byte[]>) field.get(masterKeyProvider);

      if (masterKeysMap != null) {
        masterKeysMap.put(version, newKeyBytes);
      } else {
        throw new IllegalStateException("masterKeys map is null");
      }
    } catch (Exception e) {
      log.error("Reflection failed to inject key into MasterKeyProvider", e);
      return ResponseEntity.internalServerError().build();
    }

    // Trigger the internal promotion logic (simulates discovery)
    SecurityUtils.runAsSystem(
        () -> internalMasterKeyService.promoteNewKeyInternal(version, defaultAlgo));

    return ResponseEntity.accepted().build();
  }
}
