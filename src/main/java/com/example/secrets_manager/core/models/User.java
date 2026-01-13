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
public class User {
    private UUID id;
    private String name;
    @ToString.Exclude
    private byte[] pwSalt;
    @ToString.Exclude
    private byte[] pwDigest;
    private Instant createdAt;
    private Instant modifiedAt;
    private String hashAlgo;
    private String hashParams;
    private Instant deletedAt;
}
