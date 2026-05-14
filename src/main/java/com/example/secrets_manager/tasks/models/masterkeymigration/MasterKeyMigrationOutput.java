package com.example.secrets_manager.tasks.models.masterkeymigration;

import com.example.secrets_manager.tasks.models.TaskOutput;
import com.example.secrets_manager.tasks.models.TaskTypeConstants;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Final result of a Master Key Migration task. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TaskTypeConstants.MK_MIGRATION_OUTPUT)
public class MasterKeyMigrationOutput implements TaskOutput {
  private long successfullyMigrated;
  private long totalFailures;
  private Map<UUID, String> errorDetails;
}
