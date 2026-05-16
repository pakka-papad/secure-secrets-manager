# Enabling pg_cron for Automated Maintenance

This guide explains how to install and configure the **pg_cron** extension in your PostgreSQL database. This extension is required to power automated maintenance tasks, such as cleaning up stale worker records from the `sm.worker_registry` table.

## Why pg_cron?

The application uses a distributed background task framework. To prevent the `sm.worker_registry` table from growing indefinitely with records from decommissioned nodes, the system schedules a daily cleanup job via a database-level cron trigger.

If `pg_cron` is not available, the application will still function correctly (the migration script handles its absence gracefully), but you will need to manually prune old worker records to maintain database performance.

---

## Installation & Configuration

Enabling `pg_cron` involves two steps: configuring the PostgreSQL server and creating the extension in the target database.

### 1. Server Configuration
The extension must be loaded into memory at server startup. Locate your `postgresql.conf` file and update the `shared_preload_libraries` setting:

```conf
# Add pg_cron to the list of preloaded libraries
shared_preload_libraries = 'pg_cron'

# Specify which database the cron metadata should be stored in
# This is usually the application's primary database
cron.database_name = 'secrets_manager'
```

**Explanation of Parameters:**
*   `shared_preload_libraries`: Instructs PostgreSQL to load the `pg_cron` binary during initialization.
*   `cron.database_name`: Specifies where the `cron.job` and `cron.job_run_details` tables will reside.

---

### 2. Restart PostgreSQL
Because `shared_preload_libraries` is a fundamental server setting, you must restart the PostgreSQL service for the changes to take effect.

```bash
# Example for Linux/systemd
sudo systemctl restart postgresql
```

---

### 3. Create the Extension
Connect to your database as a **superuser** (usually the `postgres` user) and execute the SQL command to enable the logic:

```sql
CREATE EXTENSION pg_cron;
```

**Post-Installation Note:**
Once the extension is created, the application's Flyway migration script (`V1__db_init.sql`) will automatically detect it and register the `cleanup-dead-workers` job.

---

## Verification

To verify that the extension is active and the maintenance job is correctly scheduled, run the following query:

```sql
SELECT * FROM cron.job;
```

**Expected Result:**
You should see a job named `cleanup-dead-workers` scheduled to run at `0 0 * * *` (midnight daily).

---

## Troubleshooting

### Error: `pg_cron must be loaded via shared_preload_libraries`
**The Cause:** You attempted to run `CREATE EXTENSION` without adding `pg_cron` to the configuration file or without restarting the server.
**The Fix:** Re-run Step 1 and Step 2, then verify the library is loaded using `SHOW shared_preload_libraries;`.

### Job Not Appearing
**The Cause:** The extension was added *after* the initial Flyway migration was already executed.
**The Fix:** Manually run the `DO` block found at the bottom of `V1__db_init.sql` or use the `cron.schedule` function to add the job manually.

---

> [!CAUTION]
> **Managed Services (RDS/Cloud SQL):** On managed platforms, you cannot modify `postgresql.conf` directly. Use the platform's **Parameter Groups** to add `pg_cron` to `shared_preload_libraries` and `cron.database_name`. Most managed providers will handle the restart for you.
