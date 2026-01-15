package com.example.secrets_manager.crypto;

import com.example.secrets_manager.crypto.dto.HashedPassword;

public interface PasswordHasher {
  String getAlgorithmName();

  HashedPassword hash(byte[] plaintextPassword);

  boolean verify(byte[] plaintextPassword, HashedPassword storedHash);
}
