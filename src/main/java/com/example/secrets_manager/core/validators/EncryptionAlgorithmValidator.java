package com.example.secrets_manager.core.validators;

import com.example.secrets_manager.crypto.CryptographyService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncryptionAlgorithmValidator
    implements ConstraintValidator<EncryptionAlgorithm, String> {

  private final CryptographyService cryptographyService;
  private EncryptionAlgorithm annotation;

  @Autowired
  public EncryptionAlgorithmValidator(CryptographyService cryptographyService) {
    this.cryptographyService = cryptographyService;
  }

  @Override
  public void initialize(EncryptionAlgorithm annotation) {
    this.annotation = annotation;
  }

  @Override
  public boolean isValid(String algorithmName, ConstraintValidatorContext context) {
    if (algorithmName == null || algorithmName.isBlank()) {
      return true; // Let @NotBlank handle it
    }
    return cryptographyService.isAlgorithmSupported(algorithmName, annotation.purpose());
  }
}
