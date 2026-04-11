package com.example.secrets_manager.crypto.impl;

import org.springframework.stereotype.Component;

/** Implementation of AES Key Wrap (AES-KW) with 128-bit key. */
@Component
public class AesKw128SymmetricCipher extends AbstractAesKwSymmetricCipher {
  @Override
  public String getAlgorithmName() {
    return "AES-KW-128";
  }

  @Override
  public int getRequiredKeySizeBytes() {
    return 16;
  }
}
