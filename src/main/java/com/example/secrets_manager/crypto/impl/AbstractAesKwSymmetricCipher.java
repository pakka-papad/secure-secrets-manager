package com.example.secrets_manager.crypto.impl;

import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.SymmetricCipher;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.EnumSet;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/** Base class for AES Key Wrap (RFC 3394) implementations. */
public abstract class AbstractAesKwSymmetricCipher implements SymmetricCipher {

  private static final String ALGORITHM_TRANSFORMATION = "AESWrap";
  private static final String KEY_ALGORITHM = "AES";

  @Override
  public Set<CipherPurpose> getSupportedPurposes() {
    return EnumSet.of(CipherPurpose.KEY_WRAP);
  }

  @Override
  public EncryptedData encrypt(byte[] plaintext, byte[] key) {
    validateKeyLength(key);
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION);
      SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGORITHM);
      cipher.init(Cipher.WRAP_MODE, keySpec);

      // AESWrap in Java Cipher expects wrap/unwrap methods, but encrypt/decrypt doFinal also works
      // for some providers. However, for AESWrap it's better to use WRAP_MODE.
      // But wrap() returns the bytes directly.
      byte[] wrappedKey = cipher.wrap(new SecretKeySpec(plaintext, "RAW"));

      return new EncryptedData(wrappedKey, null, null, getAlgorithmName());
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("Failed to wrap key with " + getAlgorithmName(), e);
    }
  }

  @Override
  public byte[] decrypt(EncryptedData data, byte[] key) throws CryptoOperationException {
    validateKeyLength(key);
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION);
      SecretKeySpec keySpec = new SecretKeySpec(key, KEY_ALGORITHM);
      cipher.init(Cipher.UNWRAP_MODE, keySpec);

      // Unwrap expects the original algorithm of the wrapped key. Since we wrap raw bytes,
      // we use "RAW".
      Key unwrappedKey = cipher.unwrap(data.getCiphertext(), "RAW", Cipher.SECRET_KEY);
      return unwrappedKey.getEncoded();
    } catch (GeneralSecurityException e) {
      throw new CryptoOperationException(
          "Failed to unwrap key. Data may be corrupt or KEK may be incorrect.", e);
    }
  }

  protected void validateKeyLength(byte[] key) {
    if (key == null || key.length != getRequiredKeySizeBytes()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid key length for %s. Key must be %d bytes.",
              getAlgorithmName(), getRequiredKeySizeBytes()));
    }
  }
}
