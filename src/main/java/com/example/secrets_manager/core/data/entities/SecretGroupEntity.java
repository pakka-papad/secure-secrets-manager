package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = CoreDataConstants.TABLE_SECRET_GROUPS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE sm.secret_groups SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class SecretGroupEntity {

  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_DATA_KEY_LENGTH = "data_key_length";
  public static final String COL_ENCRYPT_ALGO = "encrypt_algo";
  public static final String COL_CREATED_AT = "created_at";
  public static final String COL_MODIFIED_AT = "modified_at";
  public static final String COL_DELETED_AT = "deleted_at";

  @Id
  @Column(name = COL_ID)
  private UUID id;

  @Column(name = COL_NAME, nullable = false, unique = true)
  private String name;

  @Column(name = COL_DATA_KEY_LENGTH, nullable = false)
  private int dataKeyLength;

  @Column(name = COL_ENCRYPT_ALGO, nullable = false, length = 31)
  private String encryptAlgo;

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
