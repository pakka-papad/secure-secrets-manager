package com.example.secrets_manager.api.rest.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.api.rest.dto.UserCreationRequest;
import com.example.secrets_manager.core.models.UserCreationPayload;
import com.example.secrets_manager.core.models.UserRole;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class UserCreationRequestConverterTest {

  @Test
  void toModel_ShouldMapAllFieldsCorrectly() {
    UserCreationRequest request =
        UserCreationRequest.builder()
            .name("newUser")
            .password("password123")
            .roles(EnumSet.of(UserRole.ADMIN))
            .build();

    UserCreationPayload model = UserCreationRequestConverter.toModel(request);

    assertThat(model).isNotNull();
    assertThat(model.getName()).isEqualTo("newUser");
    assertThat(model.getPassword()).isEqualTo("password123".getBytes(StandardCharsets.UTF_8));
    assertThat(model.getRoles()).containsExactly(UserRole.ADMIN);
  }
}
