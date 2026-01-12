package com.example.secrets_manager.core.data.entities;

import com.example.secrets_manager.core.data.CoreDataConstants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = CoreDataConstants.TABLE_MASTER_KEYS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterKeyEntity {

    public static final String COL_VERSION = "version";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_STATUS = "status";

    @Id
    @Column(name = COL_VERSION)
    private Integer version;

    @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = COL_STATUS, nullable = false, length = 31)
    private String status;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
