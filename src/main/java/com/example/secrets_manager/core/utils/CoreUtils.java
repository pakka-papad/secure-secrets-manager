package com.example.secrets_manager.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;

public final class CoreUtils {

  private CoreUtils() {
    // Prevent instantiation
  }

  /**
   * Deserializes a JSON string into a Map of Objects.
   *
   * @param objectMapper The ObjectMapper to use for deserialization.
   * @param json The JSON string to deserialize.
   * @return A Map of Objects.
   * @throws RuntimeException if deserialization fails (wraps JsonProcessingException).
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> jsonStringToObjectMap(ObjectMapper objectMapper, String json) {
    try {
      return (Map<String, Object>) objectMapper.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to deserialize JSON map", e);
    }
  }

  public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
}
