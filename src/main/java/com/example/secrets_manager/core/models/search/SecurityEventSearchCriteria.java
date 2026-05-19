package com.example.secrets_manager.core.models.search;

import com.example.secrets_manager.core.models.SecurityEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Criteria for filtering security events during searches. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventSearchCriteria {
  private UUID actorUserId;
  private SecurityEvent action;
  private Instant startTime;
  private Instant endTime;
}
