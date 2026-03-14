package com.example.secrets_manager.crypto.dto;

/**
 * Metadata representing a supported symmetric encryption algorithm.
 *
 * @param name The human-readable name of the algorithm (e.g. "AES-256-GCM").
 * @param keySizeBytes The required key length in bytes.
 */
public record SymmetricAlgorithmMetadata(String name, int keySizeBytes) {}
