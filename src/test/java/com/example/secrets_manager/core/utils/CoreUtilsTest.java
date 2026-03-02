package com.example.secrets_manager.core.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoreUtilsTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void jsonStringToObjectMap_ShouldDeserializeCorrectly() {
    String json = "{\"key\":\"value\", \"int\":123}";
    Map<String, Object> map = CoreUtils.jsonStringToObjectMap(objectMapper, json);

    assertThat(map).isNotNull();
    assertThat(map.get("key")).isEqualTo("value");
    assertThat(map.get("int")).isEqualTo(123);
  }

  @Test
  void jsonStringToObjectMap_WithInvalidJson_ShouldThrowException() {
    String invalidJson = "{invalid}";
    assertThatThrownBy(() -> CoreUtils.jsonStringToObjectMap(objectMapper, invalidJson))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to deserialize JSON map");
  }
}
