package com.example.secrets_manager.security.forensics;

import com.example.secrets_manager.core.services.UserService;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/** Strategy for recording forensics from unauthorized user creation attempts. */
@Component
public class UserCreationForensicsStrategy implements MethodSecurityForensicStrategy {

  @Override
  public Class<?> getTargetClass() {
    return UserService.class;
  }

  @Override
  public String getMethodName() {
    return "createUser";
  }

  @Override
  public String getForensicDetails(@NonNull MethodInvocation invocation) {
    return "{\"action\":\"unauthorized_user_creation\", \"reason\":\"Admin role required\"}";
  }
}
