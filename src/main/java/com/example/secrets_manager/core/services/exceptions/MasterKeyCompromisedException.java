package com.example.secrets_manager.core.services.exceptions;

import lombok.Getter;

/**
 * Thrown when an operation attempt is made on a secret protected by a compromised master key. These
 * secrets are considered strictly blocked and unrecoverable.
 */
@Getter
public class MasterKeyCompromisedException extends SecretServiceException {
  private final int masterKeyVersion;

  public MasterKeyCompromisedException(int masterKeyVersion, String message) {
    super(message);
    this.masterKeyVersion = masterKeyVersion;
  }
}
