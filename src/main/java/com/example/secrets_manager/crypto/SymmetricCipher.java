package com.example.secrets_manager.crypto;

import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;

public interface SymmetricCipher {
  String getAlgorithmName();

  EncryptedData encrypt(byte[] plaintext, byte[] key);

  byte[] decrypt(EncryptedData data, byte[] key) throws CryptoOperationException;
}
