package com.example.secrets_manager.crypto;

/**
 * Defines the intended usage purpose for a cryptographic algorithm. Algorithms can be specialized
 * for specific use cases (like key wrapping) or suitable for general data encryption.
 */
public enum CipherPurpose {
  /** Suitable for general-purpose data encryption (e.g., secrets in Secret Groups). */
  DATA,

  /** Suitable for wrapping cryptographic keys (e.g., DEKs). */
  KEY_WRAP
}
