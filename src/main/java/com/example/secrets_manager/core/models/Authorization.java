package com.example.secrets_manager.core.models;

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
public class Authorization {
  private UUID userId;
  private UUID groupId;
  private boolean pRead;
  private boolean pWrite;
  private Instant modifiedAt;
}
