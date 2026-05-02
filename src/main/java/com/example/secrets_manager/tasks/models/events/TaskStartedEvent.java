package com.example.secrets_manager.tasks.models.events;

import java.util.UUID;

/** Event published when a task begins execution on a local worker. */
public record TaskStartedEvent(UUID taskId) {}
