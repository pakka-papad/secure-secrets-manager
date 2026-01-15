package com.example.secrets_manager.crypto.dto;

import lombok.Value;

/**
 * An immutable Data Transfer Object representing encrypted data and the metadata needed to decrypt
 * it.
 */
@Value
public class EncryptedData {
  byte[] ciphertext;
  byte[] nonce;
  byte[] authTag;
  String algorithm;
}
