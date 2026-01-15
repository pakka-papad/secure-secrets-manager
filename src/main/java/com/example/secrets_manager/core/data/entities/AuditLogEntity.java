package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = CoreDataConstants.TABLE_AUDIT_LOGS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

  public static final String COL_SEQ_ID = "seq_id";
  public static final String COL_CAUSE_SEQ_ID = "cause_seq_id";
  public static final String COL_CREATED_AT = "created_at";
  public static final String COL_ACTOR_USER_ID = "actor_user_id";
  public static final String COL_ACTION = "action";
  public static final String COL_TARGET_USER_ID = "target_user_id";
  public static final String COL_TARGET_GROUP_ID = "target_group_id";
  public static final String COL_TARGET_SECRET_ID = "target_secret_id";
  public static final String COL_TARGET_MASTER_KEY_VERSION = "target_master_key_version";
  public static final String COL_DETAILS = "details";
  public static final String COL_PREV_HASH = "prev_hash";
  public static final String COL_DATA_HASH = "data_hash";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = COL_SEQ_ID)
  private Long seqId;

  @Column(name = COL_CAUSE_SEQ_ID)
  private Long causeSeqId;

  @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = COL_ACTOR_USER_ID, nullable = false)
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
  private String details;

  @Column(name = COL_PREV_HASH, nullable = false)
  private byte[] prevHash;

  @Column(name = COL_DATA_HASH, nullable = false)
  private byte[] dataHash;
}
