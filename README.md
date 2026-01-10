# Secure Secrets Manager
*A security-focused backend system showcasing encryption, auditability, and key rotation*

---

## Overview

This project implements a **secure secrets management service** designed to store and retrieve sensitive configuration data such as API keys, credentials, and tokens.  
It emphasizes **strong encryption at rest**, **fine-grained access control**, **tamper-evident auditing**, and **safe cryptographic key rotation**.

The goal of this project is to demonstrate **security-conscious software engineering practices**. It is intentionally scoped as a single-service system and does **not** aim to be a production replacement for tools like Vault or cloud-managed secret stores.

---

## Key Features

- Encrypted secret storage (no plaintext at rest)
- Secret grouping with access control boundaries
- Hierarchical key management (Master Keys & Data Encryption Keys)
- Asynchronous key rotation (non-blocking)
- Append-only, tamper-evident audit logs
- Durable background task system
- User-authored actions preserved across automated operations

---

## High-Level Architecture

The system is composed of the following logical components:

- **API Service**  
  Handles authenticated client requests, enforces authorization, coordinates encryption, and persists state changes.

- **Cryptography Module**  
  Encapsulates all cryptographic operations such as key generation, encryption/decryption, and audit hash computation.

- **Persistent Storage (PostgreSQL)**  
  Stores encrypted secrets, encrypted data keys, audit logs, and background task state.  
  Plaintext secrets and master keys are never persisted.

- **Background Task Workers**  
  Execute long-running or resource-intensive operations (e.g., key rotation) asynchronously.

- **Authentication & Authorization Module**  
  Verifies user identity and enforces read/write permissions at the secret-group level.

The API service and workers are stateless with respect to persistent data. Master keys are injected at startup and held **only in memory**.

---

## Core Concepts

### Users
Authenticated identities that interact with the system.

### Secret Groups
Logical groupings of secrets that define:
- Encryption parameters
- Access control boundaries

### Secrets
Encrypted values identified by name and scoped to a secret group.  
Secrets do **not** retain historical values.

### Keys
- **Master Keys (MKs)**: Versioned keys used to encrypt Data Encryption Keys.
- **Data Encryption Keys (DEKs)**: Per-secret keys used to encrypt secret values.

### Audit Logs
Append-only records of all security-relevant actions, cryptographically chained to detect tampering.

### Background Tasks
Durable representations of asynchronous operations such as key rotation.

---

## Cryptographic Design

The system uses a layered encryption model:

```
Master Key (MK)
      ↓
Encrypts Data Encryption Key (DEK)
      ↓
Encrypts Secret Value
```

Key properties:
- Each secret has its own DEK
- Master keys are versioned and never persisted
- Key rotation is supported without blocking reads or writes
- Authenticated encryption (AEAD) ensures confidentiality and integrity

---

## Request Lifecycle

### Write Secret (new secret)
1. Authenticate and authorize the request
2. Generate a new DEK
3. Encrypt the secret value
4. Encrypt the DEK with the current master key
5. Persist encrypted data
6. Append an audit log entry

### Read Secret
1. Authenticate and authorize the request
2. Decrypt the DEK using the appropriate master key
3. Decrypt and return the secret value
4. Append an audit log entry

All state changes and audit logging occur atomically.

---

## Audit Logging & Integrity

- Audit logs are **append-only**
- Each entry is cryptographically chained to the previous entry
- Tampering can be detected through hash verification
- Asynchronous system actions retain references to the initiating user action

This preserves **causality** between human intent and automated system behavior.

---

## Background Task System

Some operations (e.g., DEK or MK rotation) are intentionally performed asynchronously.

Background tasks:
- Are persisted in the database
- Transition through a defined state machine
- Are executed idempotently
- Support retries and failure recovery
- Retain references to the initiating user and audit log entry

---

## Authentication & Authorization

- Users authenticate using securely hashed credentials
- Authorization is enforced at the secret-group level
- Permissions are explicitly separated into **read** and **write**
- All access checks are enforced consistently across API requests and background tasks

---

## Failure Handling & Recovery

The system is designed to tolerate partial failures:

- Worker crashes do not corrupt task state
- Tasks can be retried safely
- Key rotation can be paused and resumed
- Failures are observable via task state and audit logs

---

## Security Considerations

This project explicitly addresses:

- Database compromise (encrypted data at rest)
- Insider misuse (authorization + auditing)
- Audit log tampering (hash chaining)
- Key lifecycle management (versioning and rotation)

Security trade-offs are made consciously to balance clarity, safety, and simplicity.

---

## Non-Goals

This project does **not** attempt to solve:

- External KMS or HSM integration
- Multi-region or distributed deployments
- Client-side SDKs
- Compliance frameworks (PCI, HIPAA, etc.)
- Real-time key rotation guarantees

---

## Future Improvements

- External KMS / HSM integration
- Secret version history
- Merkle-tree–based audit verification
- More expressive access policies
- Multi-tenant isolation

---

## License

