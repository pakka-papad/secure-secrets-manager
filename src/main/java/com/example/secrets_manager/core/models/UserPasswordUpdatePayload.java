package com.example.secrets_manager.core.models;

import com.example.secrets_manager.core.validators.Password;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPasswordUpdatePayload {
  private byte[] oldPassword;
  @Password private byte[] newPassword;
}
