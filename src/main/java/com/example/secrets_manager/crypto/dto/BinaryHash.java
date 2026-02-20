package com.example.secrets_manager.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a generic cryptographic hash and the algorithm used to generate it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinaryHash {
  private String algorithm;
  private byte[] hash;
}
