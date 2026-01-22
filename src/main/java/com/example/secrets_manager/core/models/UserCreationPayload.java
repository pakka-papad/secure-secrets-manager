package com.example.secrets_manager.core.models;

import com.example.secrets_manager.core.validators.Password;
import com.example.secrets_manager.core.validators.Username;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreationPayload {
  @Username private String name;
  @Password private byte[] password;
}
