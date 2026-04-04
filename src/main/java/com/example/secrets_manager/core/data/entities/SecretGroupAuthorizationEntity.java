package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = CoreDataConstants.TABLE_SECRET_GROUP_AUTHORIZATIONS,
    schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretGroupAuthorizationEntity {

  public static final String COL_P_READ = "p_read";
  public static final String COL_P_WRITE = "p_write";
  public static final String COL_P_DELETE = "p_delete";
  public static final String COL_MODIFIED_AT = "modified_at";

  public static final Set<String> ALLOWED_SORT_FIELDS = Set.of("username");

  @EmbeddedId private SecretGroupAuthorizationId id;

  @Column(name = COL_P_READ, nullable = false)
  @Builder.Default
  private boolean pRead = false;

  @Column(name = COL_P_WRITE, nullable = false)
  @Builder.Default
  private boolean pWrite = false;

  @Column(name = COL_P_DELETE, nullable = false)
  @Builder.Default
  private boolean pDelete = false;

  @Column(name = COL_MODIFIED_AT, nullable = false)
  private Instant modifiedAt;

  @PrePersist
  @PreUpdate
  protected void onUpdate() {
    modifiedAt = Instant.now();
  }
}
