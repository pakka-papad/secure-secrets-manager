package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = CoreDataConstants.TABLE_REFRESH_TOKENS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity {

  public static final String COL_ID = "id";
  public static final String COL_USER_ID = "user_id";
  public static final String COL_TOKEN_HASH = "token_hash";
  public static final String COL_HASH_ALGO = "hash_algo";
  public static final String COL_EXPIRY_DATE = "expiry_date";

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  @Column(name = COL_ID)
  private UUID id;

  @Column(name = COL_USER_ID, nullable = false, unique = true)
  private UUID userId;

  @Column(name = COL_TOKEN_HASH, nullable = false)
  @ToString.Exclude
  private byte[] tokenHash;

  @Column(name = COL_HASH_ALGO, nullable = false, length = 31)
  private String hashAlgo;

  @Column(name = COL_EXPIRY_DATE, nullable = false)
  private Instant expiryDate;
}
