package com.example.secrets_manager.db.migration;

import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Flyway Java migration to create the first "GENESIS" audit log record if one does not exist. This
 * migration is self-contained and uses raw JDBC.
 */
@Slf4j
public class V2__CreateGenesisAuditRecord extends BaseJavaMigration {

  private static final String SYSTEM_USER_ID = "00000000-0000-0000-0000-000000000000";
  private static final AuditAction GENESIS_ACTION = AuditAction.GENESIS;
  public static final byte[] GENESIS_PREV_HASH = new byte[32]; // For SHA-256 (all zeros)

  @Override
  public void migrate(Context context) throws Exception {
    // Check if any audit logs already exist using raw JDBC.
    try (Statement select = context.getConnection().createStatement()) {
      try (ResultSet rows = select.executeQuery("SELECT COUNT(*) FROM sm.audit_logs")) {
        if (rows.next() && rows.getLong(1) > 0) {
          log.info("Audit log table is not empty. Skipping GENESIS record creation.");
          return;
        }
      }
    }

    log.info("No audit logs found. Creating GENESIS record...");

    var createdAt = Instant.now();

    // 1. Build an AuditLog object directly, for type safety and readability.
    AuditLog genesisLog =
        AuditLog.builder()
            .actorUserId(UUID.fromString(SYSTEM_USER_ID))
            .action(GENESIS_ACTION)
            .createdAt(createdAt)
            .prevHash(GENESIS_PREV_HASH)
            .build();

    // 2. Calculate its data hash using the consistent ObjectMapper approach.
    byte[] dataHash = createDataHash(genesisLog);

    // 3. Save the record using a raw JDBC PreparedStatement.
    String sql =
        "INSERT INTO sm.audit_logs (actor_user_id, action, created_at, prev_hash, data_hash) VALUES (?, ?, ?, ?, ?)";
    try (PreparedStatement statement = context.getConnection().prepareStatement(sql)) {
      statement.setObject(1, genesisLog.getActorUserId());
      statement.setString(2, genesisLog.getAction().name());
      statement.setTimestamp(3, Timestamp.from(genesisLog.getCreatedAt()));
      statement.setBytes(4, genesisLog.getPrevHash());
      statement.setBytes(5, dataHash);
      statement.execute();
    }

    log.info("GENESIS audit record created.");
  }

  /**
   * Calculates a deterministic SHA-256 hash using JSON serialization, mirroring the logic in
   * CryptographyServiceImpl.
   */
  private byte[] createDataHash(Object dataToHash) throws Exception {
    // This ObjectMapper must be configured identically to the one in the main app,
    // especially regarding date/time serialization.
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    byte[] serializedData = objectMapper.writeValueAsBytes(dataToHash);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return digest.digest(serializedData);
  }
}

