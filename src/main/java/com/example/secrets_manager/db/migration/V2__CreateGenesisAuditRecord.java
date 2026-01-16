package com.example.secrets_manager.db.migration;

import com.example.secrets_manager.core.data.converters.AuditLogEntityConverter;
import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import com.example.secrets_manager.core.data.repositories.AuditLogRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLog;
import com.example.secrets_manager.crypto.CryptographyService;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Flyway Java migration to create the first "GENESIS" audit log record if one does not exist. This
 * is implemented as a Spring Component to allow for dependency injection of services.
 */
@Component
@Slf4j
public class V2__CreateGenesisAuditRecord extends BaseJavaMigration {

  private final AuditLogRepository auditLogRepository;
  private final CryptographyService cryptographyService;

  @Autowired
  public V2__CreateGenesisAuditRecord(
      AuditLogRepository auditLogRepository, CryptographyService cryptographyService) {
    this.auditLogRepository = auditLogRepository;
    this.cryptographyService = cryptographyService;
  }

  @Override
  public void migrate(Context context) throws Exception {
    if (auditLogRepository.count() > 0) {
      log.info("Audit log table is not empty. Skipping GENESIS record creation.");
      return;
    }

    log.info("No audit logs found. Creating GENESIS record...");
    final var genesisPrevHash = new byte[32];

    AuditLog genesisLog =
        AuditLog.builder()
            .actorUserId(UUID.fromString("00000000-0000-0000-0000-000000000000")) // System User
            .action(AuditAction.GENESIS)
            .createdAt(Instant.now())
            .prevHash(genesisPrevHash)
            .build();

    // 2. Calculate its data hash using the injected crypto service.
    byte[] dataHash = cryptographyService.createDataHash(genesisLog);
    genesisLog.setDataHash(dataHash);

    // 3. Convert to an entity and save it using the injected repository.
    // This works because Spring Boot's Flyway integration runs after the DataSource
    // and JPA entities are available.
    AuditLogEntity genesisEntity = AuditLogEntityConverter.fromModel(genesisLog);
    auditLogRepository.save(genesisEntity);

    log.info("GENESIS audit record created.");
  }
}
