package com.example.secrets_manager.crypto.dto;

import lombok.ToString;
import lombok.Value;

/**
 * An immutable Data Transfer Object representing encrypted data and the metadata needed to decrypt
 * it.
 */
@Value
public class EncryptedData {
  @ToString.Exclude byte[] ciphertext;
  @ToString.Exclude byte[] nonce;
  @ToString.Exclude byte[] authTag;
  String algorithm;
}
