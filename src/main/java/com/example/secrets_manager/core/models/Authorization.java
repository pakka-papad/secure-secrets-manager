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
public class Authorization {
    private UUID userId;
    private UUID groupId;
    private boolean pRead;
    private boolean pWrite;
    private Instant modifiedAt;
}
