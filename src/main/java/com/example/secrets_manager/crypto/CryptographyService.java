package com.example.secrets_manager.crypto;

import com.example.secrets_manager.crypto.dto.BinaryHash;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;

/** Service providing high-level cryptographic operations. */
public interface CryptographyService {
  /** Hashes a plaintext password using the application's default algorithm. */
  HashedPassword hashPassword(byte[] plaintextPassword);

  /**
   * Verifies a plaintext password against a stored hash. It automatically uses the correct
   * algorithm based on the metadata stored in the HashedPassword object.
   */
  boolean verifyPassword(byte[] plaintextPassword, HashedPassword storedHash);

  /** Encrypts plaintext data using a specific key and algorithm. */
  EncryptedData encrypt(byte[] plaintext, byte[] key, String algorithmName);

  /**
   * Decrypts an EncryptedData object using a specific key. It automatically uses the correct
   * algorithm based on the metadata stored in the EncryptedData object.
   *
   * @throws CryptoOperationException if decryption fails due to an incorrect key, tampered data, or
   *     other operational issues.
   */
  byte[] decrypt(EncryptedData data, byte[] key) throws CryptoOperationException;

  /** Returns the required key size in bytes for the specified symmetric algorithm. */
  int getRequiredSymmetricKeySizeBytes(String algorithmName);

  /** Checks if a symmetric cipher algorithm is supported by the system. */
  boolean isSymmetricAlgorithmSupported(String algorithmName);

  /**
   * Calculates a standard hash (e.g., SHA-256) for a given object's data. Used for creating the
   * dataHash in the audit log.
   */
  byte[] createDataHash(Object dataToHash);

  /**
   * Hashes a high-entropy byte array (like a token) using a fast, standard algorithm.
   *
   * @param data The raw bytes to hash.
   * @return A BinaryHash object containing the hash and algorithm name.
   */
  BinaryHash hashBytes(byte[] data);
}
