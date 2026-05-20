package com.example.secrets_manager.core.models.events;

/**
 * Event published when a master key is marked as compromised. Triggers immediate in-memory eviction
 * of the key material.
 *
 * @param version The version of the compromised master key.
 */
public record MasterKeyCompromisedEvent(int version) {}
