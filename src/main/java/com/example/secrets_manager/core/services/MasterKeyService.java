package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.MasterKeyEntityConverter;
import com.example.secrets_manager.core.data.repositories.MasterKeyRepository;
import com.example.secrets_manager.core.data.repositories.MasterKeySpecifications;
import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.search.MasterKeySearchCriteria;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing the lifecycle and metadata of Master Keys. */
@Service
@Slf4j
public class MasterKeyService {

  private final MasterKeyRepository masterKeyRepository;

  @Autowired
  public MasterKeyService(MasterKeyRepository masterKeyRepository) {
    this.masterKeyRepository = masterKeyRepository;
  }

  /** Lists master keys based on the provided search criteria. Restricted to administrators. */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public List<MasterKey> listMasterKeys(MasterKeySearchCriteria criteria) {
    var spec = MasterKeySpecifications.withCriteria(criteria);
    return masterKeyRepository.findAll(spec).stream()
        .map(MasterKeyEntityConverter::toModel)
        .toList();
  }
}
