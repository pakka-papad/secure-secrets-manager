package com.example.secrets_manager.core.data;

public final class CoreDataConstants {

    private CoreDataConstants() {
        // Prevent instantiation
    }

    public static final String SCHEMA_NAME = "sm";

    public static final String TABLE_USERS = "users";
    public static final String TABLE_MASTER_KEYS = "master_keys";
    public static final String TABLE_SECRET_GROUPS = "secret_groups";
    public static final String TABLE_AUTHORIZATIONS = "authorizations";
    public static final String TABLE_SECRETS = "secrets";
    public static final String TABLE_AUDIT_LOGS = "audit_logs";
    public static final String TABLE_TASKS = "tasks";
    public static final String TABLE_SYSTEM_LOCKS = "system_locks";
}
