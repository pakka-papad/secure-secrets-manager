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
@Table(name = CoreDataConstants.TABLE_USERS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_PW_SALT = "pw_salt";
    public static final String COL_PW_DIGEST = "pw_digest";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_MODIFIED_AT = "modified_at";
    public static final String COL_HASH_ALGO = "hash_algo";
    public static final String COL_HASH_PARAMS = "hash_params";

    @Id
    @Column(name = COL_ID)
    private UUID id;

    @Column(name = COL_NAME, nullable = false, unique = true)
    private String name;

    @Column(name = COL_PW_SALT, nullable = false)
    @ToString.Exclude
    private byte[] pwSalt;

    @Column(name = COL_PW_DIGEST, nullable = false)
    @ToString.Exclude
    private byte[] pwDigest;

    @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = COL_MODIFIED_AT, nullable = false)
    private Instant modifiedAt;

    @Column(name = COL_HASH_ALGO, nullable = false, length = 31)
    private String hashAlgo;

    @Column(name = COL_HASH_PARAMS, nullable = false, columnDefinition = "jsonb")
    private String hashParams;

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
