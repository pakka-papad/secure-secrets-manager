package com.example.secrets_manager.core.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<Username, String> {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,255}$");

  @Override
  public boolean isValid(String username, ConstraintValidatorContext context) {
    if (username == null) {
      return false;
    }
    return USERNAME_PATTERN.matcher(username).matches();
  }
}
