package com.example.secrets_manager.tasks.models;

/** Defines the possible states of a background task. */
public enum TaskState {
  PENDING,
  RUNNING,
  COMPLETED,
  FAILED,
  CANCELLED
}
