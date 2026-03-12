package com.example.secrets_manager.core.models;

/** Defines the global system roles for Role-Based Access Control (RBAC). */
public enum UserRole {
  /** Global administrator with full access to all users, systems, and secrets. */
  ADMIN,

  /** A user who can create secret groups and manage permissions within them. */
  SECRET_MANAGER,

  /** Standard user with base access. Can only interact with secrets via explicit authorization. */
  USER
}
