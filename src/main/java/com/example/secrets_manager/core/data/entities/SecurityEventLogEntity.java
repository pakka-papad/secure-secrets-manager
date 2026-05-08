package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = CoreDataConstants.TABLE_SECURITY_EVENT_LOGS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventLogEntity {

  public static final String COL_ID = "id";
  public static final String COL_CREATED_AT = "created_at";
  public static final String COL_CORRELATION_ID = "correlation_id";
  public static final String COL_ACTOR_USER_ID = "actor_user_id";
  public static final String COL_ACTION = "action";
  public static final String COL_TARGET_USER_ID = "target_user_id";
  public static final String COL_TARGET_GROUP_ID = "target_group_id";
  public static final String COL_TARGET_SECRET_ID = "target_secret_id";
  public static final String COL_TARGET_MASTER_KEY_VERSION = "target_master_key_version";
  public static final String COL_DETAILS = "details";

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  @Column(name = COL_ID)
  private UUID id;

  @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = COL_CORRELATION_ID, updatable = false)
  private UUID correlationId;

  @Column(name = COL_ACTOR_USER_ID)
  private UUID actorUserId;

  @Column(name = COL_ACTION, nullable = false, length = 31)
  private String action;

  @Column(name = COL_TARGET_USER_ID)
  private UUID targetUserId;

  @Column(name = COL_TARGET_GROUP_ID)
  private UUID targetGroupId;

  @Column(name = COL_TARGET_SECRET_ID)
  private UUID targetSecretId;

  @Column(name = COL_TARGET_MASTER_KEY_VERSION)
  private Integer targetMasterKeyVersion;

  @Column(name = COL_DETAILS, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String details;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
  }
}
