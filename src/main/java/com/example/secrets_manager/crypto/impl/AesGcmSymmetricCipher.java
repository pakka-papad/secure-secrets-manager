package com.example.secrets_manager.crypto.impl;

import com.example.secrets_manager.crypto.SymmetricCipher;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AesGcmSymmetricCipher implements SymmetricCipher {

  private static final String ALGORITHM_NAME = "AES-256-GCM";
  private static final String ALGORITHM_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String KEY_ALGORITHM = "AES";
  private static final int KEY_LENGTH_BYTES = 32;
  private static final int NONCE_LENGTH_BYTES = 12;
  private static final int AUTH_TAG_LENGTH_BITS = 128;

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public String getAlgorithmName() {
    return ALGORITHM_NAME;
  }

  @Override
  public EncryptedData encrypt(byte[] plaintext, byte[] key) {
    byte[] nonce = new byte[NONCE_LENGTH_BYTES];
    secureRandom.nextBytes(nonce);

    try {
      Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, key, nonce);
      byte[] ciphertext = cipher.doFinal(plaintext);
      // For GCM, the auth tag is appended to the ciphertext by the provider.
      // There is no separate authTag byte array to manage.
      return new EncryptedData(ciphertext, nonce, null, ALGORITHM_NAME);
    } catch (GeneralSecurityException e) {
      // Encryption failures are unexpected and indicate a programming or environment error.
      throw new RuntimeException("Failed to encrypt data", e);
    }
  }

  @Override
  public byte[] decrypt(EncryptedData data, byte[] key) throws CryptoOperationException {
    try {
      Cipher cipher = initCipher(Cipher.DECRYPT_MODE, key, data.getNonce());
      return cipher.doFinal(data.getCiphertext());
    } catch (GeneralSecurityException e) {
      // Decryption failures can be expected (e.g., bad key, tampered data)
      // and should be handled by the caller.
      throw new CryptoOperationException(
          "Failed to decrypt data. Data may be corrupt or key may be incorrect.", e);
    }
  }

  private Cipher initCipher(int opMode, byte[] key, byte[] nonce) throws GeneralSecurityException {
    if (key == null || key.length != KEY_LENGTH_BYTES) {
      throw new IllegalArgumentException("Invalid key length for AES-256. Key must be 32 bytes.");
    }
    if (nonce == null || nonce.length != NONCE_LENGTH_BYTES) {
      throw new IllegalArgumentException("Invalid nonce length. Nonce must be 12 bytes for GCM.");
    }

    Cipher cipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION);
    SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGORITHM);
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, nonce);
    cipher.init(opMode, keySpec, gcmParameterSpec);
    return cipher;
  }
}
