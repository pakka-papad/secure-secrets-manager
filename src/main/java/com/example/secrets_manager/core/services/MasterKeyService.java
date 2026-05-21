package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.search.MasterKeySearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Service for administrative Master Key operations. */
@Service
@Slf4j
@RequiredArgsConstructor
public class MasterKeyService {

  private final InternalMasterKeyService internalMasterKeyService;

  /**
   * Lists all master keys based on the provided criteria.
   *
   * @param criteria The filtering criteria.
   * @param pageable The pagination parameters.
   * @return A paginated list of master key domain models.
   */
  @PreAuthorize("hasRole('ADMIN')")
  public Page<MasterKey> listMasterKeys(MasterKeySearchCriteria criteria, Pageable pageable) {
    return internalMasterKeyService.listMasterKeys(criteria, pageable);
  }

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
