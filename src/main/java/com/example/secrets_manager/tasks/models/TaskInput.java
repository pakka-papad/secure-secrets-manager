package com.example.secrets_manager.tasks.models;

import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationInput;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Base interface for all task input payloads. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = MasterKeyMigrationInput.class, name = "MASTER_KEY_MIGRATION_INPUT")
})
public interface TaskInput {}
