package com.example.secrets_manager.api.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private List<String> messages;
    private String path;
}
