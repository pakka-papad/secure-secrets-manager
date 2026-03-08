package com.example.secrets_manager.core.models;

import com.example.secrets_manager.crypto.dto.EncryptedData;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Secret {
  private UUID id;
  private UUID groupId;
  private String secretName;
  private EncryptedData valueEnvelope;
  private EncryptedData dekEnvelope;
  private int dekVersion;
  private Integer masterKeyVersion;
  private Instant createdAt;
  private Instant modifiedAt;
  private Instant deletedAt;
}
