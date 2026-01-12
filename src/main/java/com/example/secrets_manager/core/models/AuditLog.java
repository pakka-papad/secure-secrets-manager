package com.example.secrets_manager.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long seqId;
    private Long causeSeqId;
    private UUID userId;
    private String action;
    private UUID secretId;
    private Instant createdAt;
    @ToString.Exclude
    private byte[] prevHash;
    @ToString.Exclude
    private byte[] dataHash;
}
