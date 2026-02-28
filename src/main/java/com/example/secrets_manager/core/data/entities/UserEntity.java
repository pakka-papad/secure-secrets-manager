package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = CoreDataConstants.TABLE_USERS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE sm.users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class UserEntity {

  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_PW_SALT = "pw_salt";
  public static final String COL_PW_DIGEST = "pw_digest";
  public static final String COL_CREATED_AT = "created_at";
  public static final String COL_MODIFIED_AT = "modified_at";
  public static final String COL_HASH_ALGO = "hash_algo";
  public static final String COL_HASH_PARAMS = "hash_params";
  public static final String COL_ROLES = "roles";
  public static final String COL_DELETED_AT = "deleted_at";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = COL_ID)
  private UUID id;

  @Column(name = COL_NAME, nullable = false, unique = true)
  private String name;

  @Column(name = COL_PW_SALT, nullable = false)
  @ToString.Exclude
  private byte[] pwSalt;

  @Column(name = COL_PW_DIGEST, nullable = false)
  @ToString.Exclude
  private byte[] pwDigest;

  @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = COL_MODIFIED_AT, nullable = false)
  private Instant modifiedAt;

  @Column(name = COL_HASH_ALGO, nullable = false, length = 31)
  private String hashAlgo;

  @Column(name = COL_HASH_PARAMS, nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String hashParams;

  @Column(name = COL_ROLES, nullable = false, columnDefinition = "varchar(31)[]")
  private String[] roles;

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
