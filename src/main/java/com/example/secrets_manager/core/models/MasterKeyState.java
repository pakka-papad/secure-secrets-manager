package com.example.secrets_manager.core.models;

/** Represents the lifecycle state of a Master Key. */
public enum MasterKeyState {
  /** Key is fully functional and can be used for both encryption and decryption. */
  ACTIVE,

  /** Key is no longer used for new encryptions but remains available for decrypting legacy data. */
  RETIRED,

  /** Key is disabled and cannot be used for any cryptographic operation. */
  INACTIVE,

  /** Key is suspected to be compromised. Access is strictly blocked. */
  COMPROMISED
}
