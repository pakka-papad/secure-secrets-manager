package com.example.secrets_manager.api.rest.dto;

import com.example.secrets_manager.core.models.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Criteria for filtering user lists. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchCriteria {
  /** Partial match for user name. */
  private String name;

  /** Filter by a specific role. */
  private UserRole role;
}
