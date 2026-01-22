package com.example.secrets_manager.core.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PasswordValidatorTest {

  private final PasswordValidator validator = new PasswordValidator();

  @ParameterizedTest
  @MethodSource("validPasswords")
  void testValidPassword(byte[] password) {
    assertTrue(validator.isValid(password, null));
  }

  @ParameterizedTest
  @MethodSource("invalidPasswords")
  void testInvalidPassword(byte[] password) {
    assertFalse(validator.isValid(password, null));
  }

  private static Stream<Arguments> validPasswords() {
    return Stream.of(
            "validpassword",
            "validPassword123",
            "valid-password",
            "valid_password",
            "valid+password",
            "valid#password",
            "valid?password")
        .map(PasswordValidatorTest::toArguments);
  }

  private static Stream<Arguments> invalidPasswords() {
    return Stream.of("short", "invalid password", "invalid!")
        .map(PasswordValidatorTest::toArguments);
  }

  private static Arguments toArguments(String password) {
    return Arguments.of(password.getBytes(StandardCharsets.UTF_8), password);
  }
}
