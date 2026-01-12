package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = CoreDataConstants.TABLE_SECRETS, schema = CoreDataConstants.SCHEMA_NAME, uniqueConstraints = {
    @UniqueConstraint(columnNames = {SecretEntity.COL_GROUP_ID, SecretEntity.COL_SECRET_NAME})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretEntity {

    public static final String COL_ID = "id";
    public static final String COL_GROUP_ID = "group_id";
    public static final String COL_SECRET_NAME = "secret_name";
    public static final String COL_ENCRYPTED_VALUE = "encrypted_value";
    public static final String COL_DATA_ENCRYPTION_KEY = "data_encryption_key";
    public static final String COL_DATA_KEY_VERSION = "data_key_version";
    public static final String COL_MASTER_KEY_VERSION = "master_key_version";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_MODIFIED_AT = "modified_at";

    @Id
    @Column(name = COL_ID)
    private UUID id;

    @Column(name = COL_GROUP_ID, nullable = false)
    private UUID groupId;

    @Column(name = COL_SECRET_NAME, nullable = false, length = 511)
    private String secretName;

    @Column(name = COL_ENCRYPTED_VALUE, nullable = false)
    @ToString.Exclude
    private byte[] encryptedValue;

    @Column(name = COL_DATA_ENCRYPTION_KEY, nullable = false)
    @ToString.Exclude
    private byte[] dataEncryptionKey;

    @Column(name = COL_DATA_KEY_VERSION, nullable = false)
    private int dataKeyVersion;

    @Column(name = COL_MASTER_KEY_VERSION, nullable = false)
    private Integer masterKeyVersion;

    @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = COL_MODIFIED_AT, nullable = false)
    private Instant modifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        modifiedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = Instant.now();
    }
}
