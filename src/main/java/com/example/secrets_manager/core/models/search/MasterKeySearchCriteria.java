package com.example.secrets_manager.core.models.search;

import com.example.secrets_manager.core.models.MasterKeyState;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Criteria for filtering master keys. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterKeySearchCriteria {
  /** Filter by one or more statuses. */
  private Set<MasterKeyState> statuses;
}
