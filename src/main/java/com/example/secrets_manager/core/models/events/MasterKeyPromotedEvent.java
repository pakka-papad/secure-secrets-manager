package com.example.secrets_manager.core.models.events;

import java.util.List;

/**
 * Event published when a new master key has been promoted to the ACTIVE state.
 *
 * @param newVersion The version number of the newly active key.
 * @param algorithm The encryption algorithm used by the new key.
 * @param retiredVersions The list of version numbers that were retired during this promotion.
 */
public record MasterKeyPromotedEvent(
    int newVersion, String algorithm, List<Integer> retiredVersions) {}
