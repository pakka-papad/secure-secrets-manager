package com.example.secrets_manager.tasks.models.masterkeymigration;

import com.example.secrets_manager.tasks.models.TaskInput;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Input for the Master Key Migration task. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("MASTER_KEY_MIGRATION_INPUT")
public class MasterKeyMigrationInput implements TaskInput {
  private int targetMasterKeyVersion;
}
