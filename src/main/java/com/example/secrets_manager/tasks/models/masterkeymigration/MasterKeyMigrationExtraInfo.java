package com.example.secrets_manager.tasks.models.masterkeymigration;

import com.example.secrets_manager.tasks.models.TaskStateExtraInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Live progress information for Master Key Migration. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("MASTER_KEY_MIGRATION_EXTRA_INFO")
public class MasterKeyMigrationExtraInfo implements TaskStateExtraInfo {
  private long totalSecrets;
  private long processedSecrets;
  private long failureCount;
  private double completionPercentage;
}
