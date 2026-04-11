package com.example.secrets_manager.crypto.impl;

import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.SymmetricCipher;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Implementation of ChaCha20-Poly1305 authenticated encryption. ChaCha20-Poly1305 is a
 * high-performance software-based AEAD cipher.
 */
@Component
public class ChaCha20Poly1305SymmetricCipher implements SymmetricCipher {

  private static final String ALGORITHM_NAME = "CHACHA20-POLY1305";
  private static final String ALGORITHM_TRANSFORMATION = "ChaCha20-Poly1305/None/NoPadding";
  private static final String KEY_ALGORITHM = "ChaCha20";
  private static final int KEY_LENGTH_BYTES = 32;
  private static final int NONCE_LENGTH_BYTES = 12;
  private static final int AUTH_TAG_LENGTH_BYTES = 16;

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public String getAlgorithmName() {
    return ALGORITHM_NAME;
  }

  @Override
  public int getRequiredKeySizeBytes() {
    return KEY_LENGTH_BYTES;
  }

  @Override
  public Set<CipherPurpose> getSupportedPurposes() {
    return EnumSet.of(CipherPurpose.DATA, CipherPurpose.KEY_WRAP);
  }

  @Override
  public EncryptedData encrypt(byte[] plaintext, byte[] key) {
    byte[] nonce = new byte[NONCE_LENGTH_BYTES];
    secureRandom.nextBytes(nonce);

    try {
      Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, key, nonce);
      byte[] combined = cipher.doFinal(plaintext);

      // Java's ChaCha20-Poly1305 appends the 16-byte tag to the ciphertext
      int ciphertextLength = combined.length - AUTH_TAG_LENGTH_BYTES;
      byte[] ciphertext = Arrays.copyOfRange(combined, 0, ciphertextLength);
      byte[] authTag = Arrays.copyOfRange(combined, ciphertextLength, combined.length);

      return new EncryptedData(ciphertext, nonce, authTag, ALGORITHM_NAME);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("Failed to encrypt data with ChaCha20-Poly1305", e);
    }
  }

  @Override
  public byte[] decrypt(EncryptedData data, byte[] key) throws CryptoOperationException {
    try {
      // Re-combine ciphertext and authTag for the Java Cipher
      byte[] ciphertext = data.getCiphertext();
      byte[] authTag = data.getAuthTag();
      byte[] combined = new byte[ciphertext.length + authTag.length];
      System.arraycopy(ciphertext, 0, combined, 0, ciphertext.length);
      System.arraycopy(authTag, 0, combined, ciphertext.length, authTag.length);

      Cipher cipher = initCipher(Cipher.DECRYPT_MODE, key, data.getNonce());
      return cipher.doFinal(combined);
    } catch (GeneralSecurityException e) {
      throw new CryptoOperationException(
          "Failed to decrypt data. Data may be corrupt or key may be incorrect.", e);
    }
  }

  private Cipher initCipher(int opMode, byte[] key, byte[] nonce) throws GeneralSecurityException {
    if (key == null || key.length != KEY_LENGTH_BYTES) {
      throw new IllegalArgumentException("Invalid key length for ChaCha20. Key must be 32 bytes.");
    }
    if (nonce == null || nonce.length != NONCE_LENGTH_BYTES) {
      throw new IllegalArgumentException("Invalid nonce length. Nonce must be 12 bytes.");
    }

    Cipher cipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION);
    SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGORITHM);
    IvParameterSpec ivSpec = new IvParameterSpec(nonce);
    cipher.init(opMode, keySpec, ivSpec);
    return cipher;
  }
}
