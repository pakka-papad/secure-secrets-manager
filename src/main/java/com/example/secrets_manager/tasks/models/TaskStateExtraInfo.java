package com.example.secrets_manager.tasks.models;

import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationExtraInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Base interface for additional runtime state info (e.g. progress stats). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(
      value = MasterKeyMigrationExtraInfo.class,
      name = "MASTER_KEY_MIGRATION_EXTRA_INFO")
})
public interface TaskStateExtraInfo {}
