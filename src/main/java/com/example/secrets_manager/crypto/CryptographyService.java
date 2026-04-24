package com.example.secrets_manager.crypto;

import com.example.secrets_manager.crypto.dto.BinaryHash;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.crypto.dto.SymmetricAlgorithmMetadata;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import java.util.List;
import javax.crypto.SecretKey;

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
  boolean isAlgorithmSupported(String algorithmName);

  /** Checks if a symmetric cipher algorithm is supported for a specific purpose. */
  boolean isAlgorithmSupported(String algorithmName, CipherPurpose purpose);

  /** Returns a list of metadata for all supported symmetric cipher algorithms. */
  List<SymmetricAlgorithmMetadata> getSupportedAlgorithms();

  /** Returns a list of metadata for symmetric algorithms that support a specific purpose. */
  List<SymmetricAlgorithmMetadata> getSupportedAlgorithms(CipherPurpose purpose);

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

  /**
   * Generates a cryptographically secure symmetric key suitable for the specified algorithm.
   *
   * @param algorithmName The name of the algorithm (e.g., "AES-256-GCM").
   * @return A SecretKey of the correct length for the algorithm.
   * @throws IllegalArgumentException if the algorithm is not supported.
   */
  SecretKey generateKey(String algorithmName);
}
