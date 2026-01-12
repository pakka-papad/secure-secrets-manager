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
public class SecretGroup {
    private UUID id;
    private String name;
    private int dataKeyLength;
    private String encryptAlgo;
    private Instant createdAt;
    private Instant modifiedAt;
}
