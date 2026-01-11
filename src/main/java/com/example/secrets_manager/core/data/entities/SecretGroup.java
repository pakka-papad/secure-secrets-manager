package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = CoreDataConstants.TABLE_SECRET_GROUPS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretGroup {

    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_DATA_KEY_LENGTH = "data_key_length";
    public static final String COL_ENCRYPT_ALGO = "encrypt_algo";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_MODIFIED_AT = "modified_at";

    @Id
    @Column(name = COL_ID)
    private UUID id;

    @Column(name = COL_NAME, nullable = false, unique = true)
    private String name;

    @Column(name = COL_DATA_KEY_LENGTH, nullable = false)
    private int dataKeyLength;

    @Column(name = COL_ENCRYPT_ALGO, nullable = false, length = 31)
    private String encryptAlgo;

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
