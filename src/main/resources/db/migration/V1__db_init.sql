CREATE SCHEMA IF NOT EXISTS sm;

CREATE TABLE IF NOT EXISTS sm.users(
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(255) NOT NULL UNIQUE,
    pw_salt BYTEA NOT NULL,
    pw_digest BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hash_algo VARCHAR(31) NOT NULL,
    hash_params JSONB NOT NULL,
    roles VARCHAR(31)[] NOT NULL DEFAULT '{}',
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_users_roles ON sm.users USING GIN (roles);

INSERT INTO sm.users (id, name, pw_salt, pw_digest, hash_algo, hash_params, roles)
VALUES ('00000000-0000-0000-0000-000000000000', 'system', '\x', '\x', 'NONE', '{}', '{"ADMIN"}') ON CONFLICT(id) DO NOTHING;

CREATE TABLE IF NOT EXISTS sm.master_keys(
    version INT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(31) NOT NULL
);

CREATE TABLE IF NOT EXISTS sm.secret_groups(
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(255) NOT NULL UNIQUE,
    data_key_length INT NOT NULL,
    encrypt_algo VARCHAR(31) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS sm.secret_group_authorizations(
    user_id UUID NOT NULL,
    group_id UUID NOT NULL,
    p_read BOOLEAN DEFAULT FALSE NOT NULL,
    p_write BOOLEAN DEFAULT FALSE NOT NULL,
    p_delete BOOLEAN DEFAULT FALSE NOT NULL,
    modified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id, group_id)
);

ALTER TABLE sm.secret_group_authorizations ADD CONSTRAINT secret_group_authorizations_user_id_fk FOREIGN KEY (user_id) REFERENCES sm.users(id);
ALTER TABLE sm.secret_group_authorizations ADD CONSTRAINT secret_group_authorizations_group_id_fk FOREIGN KEY (group_id) REFERENCES sm.secret_groups(id);

CREATE TABLE IF NOT EXISTS sm.secrets(
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    group_id UUID NOT NULL,
    secret_name VARCHAR(511) NOT NULL,
    encrypted_value BYTEA NOT NULL,
    data_encryption_key BYTEA NOT NULL,
    data_key_version INT NOT NULL,
    master_key_version INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

ALTER TABLE sm.secrets ADD CONSTRAINT secrets_group_id_fk FOREIGN KEY (group_id) REFERENCES sm.secret_groups(id);
ALTER TABLE sm.secrets ADD CONSTRAINT secrets_master_key_version_fk FOREIGN KEY (master_key_version) REFERENCES sm.master_keys(version);
ALTER TABLE sm.secrets ADD CONSTRAINT secrets_group_id_secret_name_unique UNIQUE (group_id, secret_name);

CREATE TABLE IF NOT EXISTS sm.audit_logs(
    seq_id BIGSERIAL PRIMARY KEY,
    cause_seq_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_user_id UUID NOT NULL,
    action VARCHAR(31) NOT NULL,
    target_user_id UUID NULL,
    target_group_id UUID NULL,
    target_secret_id UUID NULL,
    target_master_key_version INT NULL,
    details JSONB NULL,
    prev_hash BYTEA NOT NULL,
    data_hash BYTEA NOT NULL
);

ALTER TABLE sm.audit_logs ADD CONSTRAINT audit_logs_actor_user_id_fk FOREIGN KEY (actor_user_id) REFERENCES sm.users(id);
ALTER TABLE sm.audit_logs ADD CONSTRAINT audit_logs_target_user_id_fk FOREIGN KEY (target_user_id) REFERENCES sm.users(id);
ALTER TABLE sm.audit_logs ADD CONSTRAINT audit_logs_target_group_id_fk FOREIGN KEY (target_group_id) REFERENCES sm.secret_groups(id);
ALTER TABLE sm.audit_logs ADD CONSTRAINT audit_logs_target_secret_id_fk FOREIGN KEY (target_secret_id) REFERENCES sm.secrets(id);
ALTER TABLE sm.audit_logs ADD CONSTRAINT audit_logs_target_master_key_version_fk FOREIGN KEY (target_master_key_version) REFERENCES sm.master_keys(version);

CREATE TABLE IF NOT EXISTS sm.security_event_logs(
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_user_id UUID NULL,
    action VARCHAR(31) NOT NULL,
    target_user_id UUID NULL,
    target_group_id UUID NULL,
    target_secret_id UUID NULL,
    target_master_key_version INT NULL,
    details JSONB NULL
);

ALTER TABLE sm.security_event_logs ADD CONSTRAINT security_event_logs_actor_user_id_fk FOREIGN KEY (actor_user_id) REFERENCES sm.users(id);
ALTER TABLE sm.security_event_logs ADD CONSTRAINT security_event_logs_target_user_id_fk FOREIGN KEY (target_user_id) REFERENCES sm.users(id);
ALTER TABLE sm.security_event_logs ADD CONSTRAINT security_event_logs_target_group_id_fk FOREIGN KEY (target_group_id) REFERENCES sm.secret_groups(id);
ALTER TABLE sm.security_event_logs ADD CONSTRAINT security_event_logs_target_secret_id_fk FOREIGN KEY (target_secret_id) REFERENCES sm.secrets(id);
ALTER TABLE sm.security_event_logs ADD CONSTRAINT security_event_logs_target_master_key_version_fk FOREIGN KEY (target_master_key_version) REFERENCES sm.master_keys(version);

CREATE TABLE IF NOT EXISTS sm.tasks(
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    parent_task_id UUID,
    initiator_user_id UUID NOT NULL,
    initiator_audit_seq_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    type VARCHAR(31) NOT NULL,
    task_input JSONB,
    state VARCHAR(31) NOT NULL,
    state_extra_info JSONB,
    task_output JSONB,
    metadata JSONB
);

ALTER TABLE sm.tasks ADD CONSTRAINT tasks_initiator_user_id_fk FOREIGN KEY (initiator_user_id) REFERENCES sm.users(id);
ALTER TABLE sm.tasks ADD CONSTRAINT tasks_initiator_audit_seq_id_fk FOREIGN KEY (initiator_audit_seq_id) REFERENCES sm.audit_logs(seq_id);
ALTER TABLE sm.tasks ADD CONSTRAINT tasks_parent_task_id_fk FOREIGN KEY (parent_task_id) REFERENCES sm.tasks(id);

CREATE TABLE IF NOT EXISTS sm.system_locks (
    lock_name VARCHAR(255) PRIMARY KEY,
    description TEXT NULL
);

INSERT INTO sm.system_locks (lock_name, description) VALUES ('AUDIT_LOG_CHAIN', 'Serializes access to the audit log chain.') ON CONFLICT (lock_name) DO NOTHING;

CREATE TABLE IF NOT EXISTS sm.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL UNIQUE,
    token_hash BYTEA NOT NULL UNIQUE,
    hash_algo VARCHAR(31) NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL
);

ALTER TABLE sm.refresh_tokens ADD CONSTRAINT refresh_tokens_user_id_fk FOREIGN KEY (user_id) REFERENCES sm.users(id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON sm.refresh_tokens (user_id);
