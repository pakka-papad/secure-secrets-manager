package com.example.secrets_manager.tasks.data.repositories;

import java.util.UUID;

/** Minimal projection for task polling and capability filtering. */
public interface TaskCandidate {
  UUID getId();

  String getType();
}
