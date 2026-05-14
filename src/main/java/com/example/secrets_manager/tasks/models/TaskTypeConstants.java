package com.example.secrets_manager.tasks.models;

/**
 * Central registry of string constants used for task-related JSON polymorphism. These constants are
 * used as @JsonTypeName and @JsonSubTypes identifiers.
 */
public final class TaskTypeConstants {

  private TaskTypeConstants() {
    // Prevent instantiation
  }

  // --- Master Key Migration ---
  public static final String MK_MIGRATION_INPUT = "MASTER_KEY_MIGRATION_INPUT";
  public static final String MK_MIGRATION_OUTPUT = "MASTER_KEY_MIGRATION_OUTPUT";
  public static final String MK_MIGRATION_EXTRA_INFO = "MASTER_KEY_MIGRATION_EXTRA_INFO";
}
