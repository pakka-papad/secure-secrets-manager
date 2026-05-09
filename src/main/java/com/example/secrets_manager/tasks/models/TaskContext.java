package com.example.secrets_manager.tasks.models;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Contextual object provided to a TaskHandler during execution. Bridges the gap between generic
 * task metadata and specific execution logic.
 *
 * @param <I> The specific TaskInput type.
 */
public record TaskContext<I extends TaskInput>(
    UUID taskId, I input, Consumer<TaskStateExtraInfo> progressUpdater) {}
