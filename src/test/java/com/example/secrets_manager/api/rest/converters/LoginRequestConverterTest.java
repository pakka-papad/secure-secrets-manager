package com.example.secrets_manager.api.rest.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.api.rest.dto.LoginRequest;
import com.example.secrets_manager.core.models.LoginPayload;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LoginRequestConverterTest {

  @Test
  void toModel_ShouldMapAllFieldsCorrectly() {
    LoginRequest request =
        LoginRequest.builder().username("testUser").password("testPassword").build();

    LoginPayload model = LoginRequestConverter.toModel(request);

    assertThat(model).isNotNull();
    assertThat(model.getUsername()).isEqualTo("testUser");
    assertThat(model.getPassword()).isEqualTo("testPassword".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void toModel_WithNullRequest_ShouldReturnNull() {
    assertThat(LoginRequestConverter.toModel(null)).isNull();
  }

  @Test
  void toModel_WithNullPassword_ShouldReturnNullPassword() {
    LoginRequest request = LoginRequest.builder().username("user").password(null).build();

    LoginPayload model = LoginRequestConverter.toModel(request);

    assertThat(model.getPassword()).isNull();
  }
}
