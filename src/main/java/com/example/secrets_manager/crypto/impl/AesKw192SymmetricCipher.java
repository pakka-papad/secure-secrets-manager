package com.example.secrets_manager.crypto.impl;

import org.springframework.stereotype.Component;

/** Implementation of AES Key Wrap (AES-KW) with 192-bit key. */
@Component
public class AesKw192SymmetricCipher extends AbstractAesKwSymmetricCipher {
  @Override
  public String getAlgorithmName() {
    return "AES-KW-192";
  }

  @Override
  public int getRequiredKeySizeBytes() {
    return 24;
  }
}
