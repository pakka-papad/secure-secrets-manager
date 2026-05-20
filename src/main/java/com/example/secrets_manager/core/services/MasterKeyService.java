package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.models.MasterKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Service for administrative Master Key operations. */
@Service
@Slf4j
@RequiredArgsConstructor
public class MasterKeyService {

  private final InternalMasterKeyService internalMasterKeyService;

  /**
   * Marks a master key as compromised.
   *
   * @param version The version to mark.
   * @return The updated master key metadata.
   */
  @PreAuthorize("hasRole('ADMIN')")
  public MasterKey markKeyAsCompromised(int version) {
    return internalMasterKeyService.markKeyAsCompromised(version);
  }
}
