package com.example.secrets_manager.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private UUID id;
    private UUID parentTaskId;
    private UUID initiatorUserId;
    private Long initiatorAuditSeqId;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String type;
    private String taskInput;
    private String state;
    private String stateExtraInfo;
    private String taskOutput;
    private String metadata;
}
