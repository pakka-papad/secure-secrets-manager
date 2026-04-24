package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.MasterKeyEntityConverter;
import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import com.example.secrets_manager.core.data.repositories.MasterKeyRepository;
import com.example.secrets_manager.core.data.repositories.MasterKeySpecifications;
import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.core.models.search.MasterKeySearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal service for high-privilege Master Key operations. This service should NEVER be injected
 * into Controllers.
 */
@Service
@Slf4j
public class InternalMasterKeyService {

  private final MasterKeyRepository masterKeyRepository;

  @Autowired
  public InternalMasterKeyService(MasterKeyRepository masterKeyRepository) {
    this.masterKeyRepository = masterKeyRepository;
  }

  /** Lists master keys based on the provided search criteria. Internal use only. */
  @Transactional(readOnly = true)
  public List<MasterKey> listMasterKeys(MasterKeySearchCriteria criteria) {
    var spec = MasterKeySpecifications.withCriteria(criteria);
    return masterKeyRepository.findAll(spec).stream()
        .map(MasterKeyEntityConverter::toModel)
        .toList();
  }

  /**
   * Returns the highest version of master key present in the database. Returns zero if there are
   * none.
   */
  public int getHighestMasterKeyVersion() {
    return masterKeyRepository.findMaxVersion().orElse(0);
  }

  /** Retrieves metadata for a specific master key version. */
  @Transactional(readOnly = true)
  public MasterKey getMasterKeyMetadata(Integer version) {
    return masterKeyRepository
        .findById(version)
        .map(MasterKeyEntityConverter::toModel)
        .orElseThrow(() -> new EntityNotFoundException("Master key version not found: " + version));
  }

  /** Atomically retires existing active keys and registers a new active key. Internal use only. */
  @Transactional
  public MasterKey promoteNewKeyInternal(int version, String algorithm) {
    // 1. Retire existing active keys
    masterKeyRepository.updateStatusByStatus(
        MasterKeyState.RETIRED.name(), MasterKeyState.ACTIVE.name());

    // 2. Register the new key
    var entity =
        MasterKeyEntity.builder()
            .version(version)
            .status(MasterKeyState.ACTIVE.name())
            .encryptAlgo(algorithm)
            .build();

    var saved = masterKeyRepository.save(entity);
    log.info("Registered new Master Key v{} as ACTIVE using algorithm {}.", version, algorithm);

    return MasterKeyEntityConverter.toModel(saved);
  }
}
