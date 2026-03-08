package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(
    name = CoreDataConstants.TABLE_SECRETS,
    schema = CoreDataConstants.SCHEMA_NAME,
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {SecretEntity.COL_GROUP_ID, SecretEntity.COL_SECRET_NAME})
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE sm.secrets SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class SecretEntity {

  public static final String COL_ID = "id";
  public static final String COL_GROUP_ID = "group_id";
  public static final String COL_SECRET_NAME = "secret_name";
  public static final String COL_VALUE_CIPHERTEXT = "value_ciphertext";
  public static final String COL_VALUE_NONCE = "value_nonce";
  public static final String COL_VALUE_AUTH_TAG = "value_auth_tag";
  public static final String COL_DEK_CIPHERTEXT = "dek_ciphertext";
  public static final String COL_DEK_NONCE = "dek_nonce";
  public static final String COL_DEK_AUTH_TAG = "dek_auth_tag";
  public static final String COL_DEK_VERSION = "dek_version";
  public static final String COL_MASTER_KEY_VERSION = "master_key_version";
  public static final String COL_CREATED_AT = "created_at";
  public static final String COL_MODIFIED_AT = "modified_at";
  public static final String COL_DELETED_AT = "deleted_at";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = COL_ID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = COL_GROUP_ID, insertable = false, updatable = false)
  private SecretGroupEntity group;

  @Column(name = COL_GROUP_ID, nullable = false)
  private UUID groupId;

  @Column(name = COL_SECRET_NAME, nullable = false, length = 511)
  private String secretName;

  @Column(name = COL_VALUE_CIPHERTEXT, nullable = false)
  @ToString.Exclude
  private byte[] valueCiphertext;

  @Column(name = COL_VALUE_NONCE, nullable = false)
  @ToString.Exclude
  private byte[] valueNonce;

  @Column(name = COL_VALUE_AUTH_TAG, nullable = false)
  @ToString.Exclude
  private byte[] valueAuthTag;

  @Column(name = COL_DEK_CIPHERTEXT, nullable = false)
  @ToString.Exclude
  private byte[] dekCiphertext;

  @Column(name = COL_DEK_NONCE, nullable = false)
  @ToString.Exclude
  private byte[] dekNonce;

  @Column(name = COL_DEK_AUTH_TAG, nullable = false)
  @ToString.Exclude
  private byte[] dekAuthTag;

  @Column(name = COL_DEK_VERSION, nullable = false)
  private int dekVersion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = COL_MASTER_KEY_VERSION, insertable = false, updatable = false)
  private MasterKeyEntity masterKey;

  @Column(name = COL_MASTER_KEY_VERSION, nullable = false)
  private Integer masterKeyVersion;

  @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = COL_MODIFIED_AT, nullable = false)
  private Instant modifiedAt;

  @Column(name = COL_DELETED_AT)
  private Instant deletedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
    modifiedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    modifiedAt = Instant.now();
  }
}
