package com.example.secrets_manager.crypto.dto;

import com.example.secrets_manager.crypto.CipherPurpose;
import java.util.Set;

/**
 * Metadata representing a supported symmetric encryption algorithm.
 *
 * @param name The human-readable name of the algorithm (e.g. "AES-256-GCM").
 * @param keySizeBytes The required key length in bytes.
 * @param supportedPurposes The set of purposes this algorithm can be used for.
 */
public record SymmetricAlgorithmMetadata(
    String name, int keySizeBytes, Set<CipherPurpose> supportedPurposes) {}
