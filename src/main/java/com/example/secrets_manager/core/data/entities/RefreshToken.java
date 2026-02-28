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
@Table(name = CoreDataConstants.TABLE_REFRESH_TOKENS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
  public static final String COL_ID = "id";
  public static final String COL_USER_ID = "user_id";
  public static final String COL_TOKEN_HASH = "token_hash";
  public static final String COL_HASH_ALGO = "hash_algo";
  public static final String COL_EXPIRY_DATE = "expiry_date";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = COL_ID)
  private UUID id;

  @Column(name = COL_USER_ID, nullable = false)
  private UUID userId;

  @Column(name = COL_TOKEN_HASH, nullable = false, unique = true)
  private byte[] tokenHash;

  @Column(name = COL_HASH_ALGO, nullable = false, length = 31)
  private String hashAlgo;

  @Column(name = COL_EXPIRY_DATE, nullable = false)
  private Instant expiryDate;
}
