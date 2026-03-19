package com.example.secrets_manager.core.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UsernameValidatorTest {

  private final UsernameValidator validator = new UsernameValidator();

  @ParameterizedTest
  @ValueSource(strings = {"validusername", "validUsername123", "a", "1"})
  void testValidUsername(String username) {
    assertTrue(validator.isValid(username, null));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "invalid username",
        "invalid=username",
        "invalid@username",
        "invalid!",
        "loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong"
      })
  void testInvalidUsername(String username) {
    assertFalse(validator.isValid(username, null));
  }
}
