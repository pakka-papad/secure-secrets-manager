package com.example.secrets_manager.core.services;

import java.security.KeyPair;

/** Strategy interface for providing the EC KeyPair used for JWT signing and verification. */
public interface JwtKeyPairProvider {
  KeyPair getKeyPair();
}
