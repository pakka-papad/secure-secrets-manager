package com.example.secrets_manager.core.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<Password, byte[]> {

  private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-+#?!]{8,}$");

  @Override
  public boolean isValid(byte[] password, ConstraintValidatorContext context) {
    if (password == null) {
      return false;
    }
    String passwordStr = new String(password, StandardCharsets.UTF_8);
    return PASSWORD_PATTERN.matcher(passwordStr).matches();
  }
}
