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
@Table(name = CoreDataConstants.TABLE_AUDIT_LOGS, schema = CoreDataConstants.SCHEMA_NAME)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    public static final String COL_SEQ_ID = "seq_id";
    public static final String COL_CAUSE_SEQ_ID = "cause_seq_id";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_ACTION = "action";
    public static final String COL_SECRET_ID = "secret_id";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_PREV_HASH = "prev_hash";
    public static final String COL_DATA_HASH = "data_hash";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = COL_SEQ_ID)
    private Long seqId;

    @Column(name = COL_CAUSE_SEQ_ID)
    private Long causeSeqId;

    @Column(name = COL_USER_ID, nullable = false)
    private UUID userId;

    @Column(name = COL_ACTION, nullable = false, length = 31)
    private String action;

    @Column(name = COL_SECRET_ID, nullable = false)
    private UUID secretId;

    @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = COL_PREV_HASH, nullable = false)
    private byte[] prevHash;

    @Column(name = COL_DATA_HASH, nullable = false)
    private byte[] dataHash;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
