package com.example.secrets_manager.crypto.dto;

import java.util.Map;
import lombok.Value;

/** An immutable Data Transfer Object representing a hashed password and its metadata. */
@Value
public class HashedPassword {
  byte[] digest;
  byte[] salt;
  String algorithm;
  Map<String, Object> params;
}
