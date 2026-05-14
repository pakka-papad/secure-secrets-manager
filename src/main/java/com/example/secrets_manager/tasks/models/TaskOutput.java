package com.example.secrets_manager.tasks.models;

import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationOutput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Base interface for all task output payloads. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = MasterKeyMigrationOutput.class, name = "MASTER_KEY_MIGRATION_OUTPUT")
})
public interface TaskOutput {}
