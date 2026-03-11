package com.example.secrets_manager.core.models;

/** Represents the lifecycle state of a Master Key. */
public enum MasterKeyState {
  /**
   * The current standard key. Used for all new encryption operations and available for decryption.
   * Only the highest version master key in the system should be in this state.
   */
  ACTIVE,

  /**
   * A legacy key that is being phased out. It is no longer used for new encryptions but remains
   * available for user-initiated decryption. The background rotation job uses this state to
   * identify secrets that need their DEKs re-wrapped using the current ACTIVE key.
   */
  RETIRED,

  /**
   * A key that is no longer associated with any secrets. It cannot be used for any cryptographic
   * operation. This is the terminal state indicating that an administrator can safely remove the
   * key material from the OS environment variables.
   */
  INACTIVE,

  /**
   * Key material is suspected to be leaked. Strictly blocked for all decryption attempts by users
   * and the system. Secrets using this key are considered toxic; they cannot be migrated and must
   * be replaced with brand-new values using the ACTIVE key.
   */
  COMPROMISED
}
