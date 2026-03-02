package com.example.secrets_manager.api.rest.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.api.rest.dto.UserPasswordUpdateRequest;
import com.example.secrets_manager.core.models.UserPasswordUpdatePayload;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UserPasswordUpdateRequestConverterTest {

  @Test
  void toModel_ShouldMapAllFieldsCorrectly() {
    UserPasswordUpdateRequest request =
        UserPasswordUpdateRequest.builder()
            .oldPassword("oldPass")
            .newPassword("newPass123")
            .build();

    UserPasswordUpdatePayload model = UserPasswordUpdateRequestConverter.toModel(request);

    assertThat(model).isNotNull();
    assertThat(model.getOldPassword()).isEqualTo("oldPass".getBytes(StandardCharsets.UTF_8));
    assertThat(model.getNewPassword()).isEqualTo("newPass123".getBytes(StandardCharsets.UTF_8));
  }
}
