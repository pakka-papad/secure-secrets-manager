package com.example.secrets_manager.tasks.data.repositories;

import java.util.UUID;

/**
 * Minimal projection for stale-task polling, including the ownership that was observed as stale.
 */
public interface StaleTaskCandidate extends TaskCandidate {
  UUID getWorkerId();
}
