package com.example.secrets_manager.tasks.models;

/**
 * Defines the possible states of a background task and its execution lifecycle.
 *
 * <pre>
 *                    (Started)              (Success)
 *     [*] --> PENDING ----------> RUNNING -----------> COMPLETED --> [*]
 *               |                 /     \
 *            (Cancel)        (Error)   (Cancel)
 *               |              /           \
 *               v             v             v
 *           CANCELLED      FAILED       CANCELLED
 *               |             |             |
 *               |             v             |
 *               |            [*]            |
 *               +---------------------------+
 *               |
 *               v
 *              [*]
 * </pre>
 */
public enum TaskState {
  /** Task is created but not yet claimed by any worker. */
  PENDING,

  /** Task is actively being processed by a worker. */
  RUNNING,

  /** Task finished successfully and produced an output. */
  COMPLETED,

  /** Task execution failed due to an unhandled exception or critical error. */
  FAILED,

  /** Task was explicitly stopped by an administrator before completion. */
  CANCELLED
}
