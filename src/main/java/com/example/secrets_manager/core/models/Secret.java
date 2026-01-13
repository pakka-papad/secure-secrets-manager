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
public class Secret {
    private UUID id;
    private UUID groupId;
    private String secretName;
    @ToString.Exclude
    private byte[] encryptedValue;
    @ToString.Exclude
    private byte[] dataEncryptionKey;
    private int dataKeyVersion;
    private Integer masterKeyVersion;
    private Instant createdAt;
    private Instant modifiedAt;
    private Instant deletedAt;
}
