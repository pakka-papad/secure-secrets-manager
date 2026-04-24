package com.example.secrets_manager.core.models.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Criteria for searching/filtering secrets. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretSearchCriteria {
  /** Filter secrets whose names start with this prefix. */
  private String namePrefix;
}
