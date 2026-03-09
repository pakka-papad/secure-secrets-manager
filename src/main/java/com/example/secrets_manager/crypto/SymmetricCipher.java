package com.example.secrets_manager.crypto;

import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;

/** Interface for symmetric cryptographic operations. */
public interface SymmetricCipher {
  /** Returns the human-readable name of the algorithm (e.g., "AES-256-GCM"). */
  String getAlgorithmName();

  /** Returns the required key size in bytes for this algorithm. */
  int getRequiredKeySizeBytes();

  /** Encrypts the provided plaintext using the given key. */
  EncryptedData encrypt(byte[] plaintext, byte[] key);

  /** Decrypts the provided ciphertext using the given key. */
  byte[] decrypt(EncryptedData data, byte[] key) throws CryptoOperationException;
}
