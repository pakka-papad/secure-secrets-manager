package com.example.secrets_manager.security.forensics;

import com.example.secrets_manager.core.models.UserPasswordUpdatePayload;
import com.example.secrets_manager.core.services.UserService;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** Strategy for extracting forensics from unauthorized password update attempts. */
@Component
public class PasswordUpdateForensicsStrategy implements MethodSecurityForensicStrategy {

  @Override
  public Class<?> getTargetClass() {
    return UserService.class;
  }

  @Override
  public String getMethodName() {
    return "updatePassword";
  }

  @Override
  public @Nullable String getForensicDetails(@NonNull MethodInvocation invocation) {
    Object[] args = invocation.getArguments();
    if (args.length > 0 && args[0] instanceof UserPasswordUpdatePayload payload) {
      return String.format(
          "{\"action\":\"unauthorized_password_update\", \"attempted_target_id\":\"%s\", \"reason\":\"Ownership check failed\"}",
          payload.getUserId());
    }
    return null;
  }
}
