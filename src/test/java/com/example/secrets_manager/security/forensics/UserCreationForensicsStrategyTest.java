package com.example.secrets_manager.security.forensics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.secrets_manager.core.services.UserService;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

class UserCreationForensicsStrategyTest {

  private final UserCreationForensicsStrategy strategy = new UserCreationForensicsStrategy();

  @Test
  void getMethodName_ShouldReturnCorrectName() {
    assertThat(strategy.getMethodName()).isEqualTo("createUser");
  }

  @Test
  void getTargetClass_ShouldReturnUserService() {
    assertThat(strategy.getTargetClass()).isEqualTo(UserService.class);
  }

  @Test
  void getForensicDetails_ShouldReturnAdminRequiredMessage() {
    MethodInvocation invocation = mock(MethodInvocation.class);
    String details = strategy.getForensicDetails(invocation);

    assertThat(details).contains("unauthorized_user_creation");
    assertThat(details).contains("Admin role required");
  }
}
