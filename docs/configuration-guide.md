# Configuration Guide

This document details all configuration options available in the platform. Configuration is primarily managed via environment variables and the `application.yml` file.

---

## 1. Database Infrastructure (PostgreSQL)

| Property | Environment Variable | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `spring.datasource.url` | `POSTGRES_DB` (Partial) | **Yes** | - | The database name in the JDBC URL. |
| `spring.datasource.username` | `POSTGRES_USER` | **Yes** | - | Database user name. |
| `spring.datasource.password` | `POSTGRES_PASSWORD` | **Yes** | - | Database user password. |

---

## 2. Cryptographic Root of Trust

### Master Keys
The system uses a versioned pattern for Master Key injection. At least one key must be provided to initialize the system. For detailed generation instructions, see the [Master Key Generation Guide](creating-master-keys.md).

| Property Pattern | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `MASTER_KEY__V{n}` | **Yes** | - | High-entropy random bytes (Base64 encoded). `{n}` must be a positive integer (e.g., `MASTER_KEY__V1`). |
| `MASTER_KEY_DEFAULT_ALGORITHM` | No | `AES-256-GCM` | The algorithm used for newly promoted master keys. Supported: `AES-256-GCM`, `ChaCha20-Poly1305`, `AES-KW-128`, `AES-KW-192`, `AES-KW-256`. |

### JWT (Authentication)
Key pairs must be generated using the Elliptic Curve P-256 (prime256v1) algorithm. For detailed generation instructions, see the [JWT Key Generation Guide](creating-jwt-keys.md).

| Property | Environment Variable | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `jwt.secret-key.private` | `JWT_PRIVATE_KEY` | **Yes** | - | Base64 encoded PKCS#8 EC Private Key. |
| `jwt.secret-key.public` | `JWT_PUBLIC_KEY` | **Yes** | - | Base64 encoded X.509 EC Public Key. |
| `jwt.expiration.access` | - | No | `300000` | Access token lifespan in milliseconds (5 mins). |
| `jwt.expiration.refresh` | - | No | `2592000000` | Refresh token lifespan in milliseconds (30 days). |

---

## 3. Background Task Engine

These settings control the distributed worker performance and distributed locking behavior.

| Property | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `tasks.polling.batch-size` | No | `50` | Number of tasks a single worker will attempt to claim in one poll cycle. |
| `tasks.polling.candidate-limit`| No | `200` | Number of tasks fetched from DB for application-side capability filtering. |
| `tasks.polling.pending-interval`| No | `30000` | Delay between polls for new pending tasks (milliseconds). |
| `tasks.polling.stale-interval`  | No | `60000` | Delay between polls for abandoned (stale) tasks (milliseconds). |
| `tasks.staleness.threshold`     | No | `60s` | Duration of heartbeat silence before a task is considered abandoned. |
| `tasks.heartbeat.throttle-ms`   | No | `5000` | Minimum interval between database heartbeat updates per worker. |

---

## 4. System & Security

| Property | Required | Default | Description |
| :--- | :--- | :--- | :--- |
| `bootstrap.admin.username` | No | `admin` | Initial admin username if the user table is empty. |
| `bootstrap.admin.password` | No | (Random) | Initial admin password. Check logs on first startup if not provided. |
| `tracing.trust-external-id`| No | `false` | If true, the system will accept `X-Correlation-ID` from incoming HTTP headers. |
| `bucket4j.enabled`         | No | `true` | Enables/Disables the API rate limiting filters. |

---

## 5. Performance Considerations

*   **Task Batch Size**: Increasing `tasks.polling.batch-size` increases throughput but increases memory pressure on individual nodes.
*   **Heartbeat Throttling**: The `tasks.heartbeat.throttle-ms` prevents the database from being overwhelmed by heartbeat writes in large clusters. Increasing this value requires a proportional increase in `tasks.staleness.threshold` to prevent accidental task reclamation.
