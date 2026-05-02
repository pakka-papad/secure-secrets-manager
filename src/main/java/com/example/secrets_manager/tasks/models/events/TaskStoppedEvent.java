package com.example.secrets_manager.tasks.models.events;

import java.util.UUID;

/**
 * Event published when a task stops execution on a local worker (success, failure, or eviction).
 */
public record TaskStoppedEvent(UUID taskId) {}
