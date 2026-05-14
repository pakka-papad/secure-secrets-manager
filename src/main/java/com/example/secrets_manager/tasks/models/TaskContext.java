package com.example.secrets_manager.tasks.models;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Contextual object provided to a TaskHandler during execution. Bridges the gap between generic
 * task metadata and specific execution logic.
 *
 * @param <I> The specific TaskInput type.
 * @param <E> The specific TaskStateExtraInfo type.
 */
public record TaskContext<I extends TaskInput, E extends TaskStateExtraInfo>(
    UUID taskId, I input, Consumer<E> progressUpdater) {}
