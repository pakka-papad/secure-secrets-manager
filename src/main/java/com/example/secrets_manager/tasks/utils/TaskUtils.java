package com.example.secrets_manager.tasks.utils;

import java.util.UUID;

public class TaskUtils {

  private TaskUtils() {}

  /** The unique, immutable identity of this service instance. */
  public static final UUID WORKER_ID = UUID.randomUUID();
}
