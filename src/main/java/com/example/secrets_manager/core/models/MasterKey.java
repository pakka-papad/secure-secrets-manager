package com.example.secrets_manager.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterKey {
    private Integer version;
    private Instant createdAt;
    private String status;
}
