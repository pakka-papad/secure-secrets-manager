package com.example.secrets_manager.tasks.models;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Criteria for filtering background tasks during searches. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSearchCriteria {
  private Set<TaskType> types;
  private Set<TaskState> states;
}
