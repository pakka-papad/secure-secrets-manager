package com.example.secrets_manager.api.rest.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
  private Instant timestamp;
  private int status;
  private String error;
  private List<String> messages;
  private String path;
}
