package com.example.secrets_manager.core.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.CryptographyService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncryptionAlgorithmValidatorTest {

  @Mock private CryptographyService cryptographyService;
  @Mock private EncryptionAlgorithm annotation;
  @Mock private ConstraintValidatorContext context;

  private EncryptionAlgorithmValidator validator;

  @BeforeEach
  void setUp() {
    validator = new EncryptionAlgorithmValidator(cryptographyService);
    // Default to DATA purpose
    lenient().when(annotation.purpose()).thenReturn(CipherPurpose.DATA);
    validator.initialize(annotation);
  }

  @Test
  void isValid_ShouldReturnTrueForNullOrBlank() {
    assertThat(validator.isValid(null, context)).isTrue();
    assertThat(validator.isValid("", context)).isTrue();
    assertThat(validator.isValid("   ", context)).isTrue();
  }

  @Test
  void isValid_ShouldReturnTrueForSupportedAlgorithm() {
    String algo = "AES-256-GCM";
    when(cryptographyService.isAlgorithmSupported(algo, CipherPurpose.DATA)).thenReturn(true);

    assertThat(validator.isValid(algo, context)).isTrue();
  }

  @Test
  void isValid_ShouldReturnFalseForUnsupportedAlgorithm() {
    String algo = "AES-KW-256";
    when(cryptographyService.isAlgorithmSupported(algo, CipherPurpose.DATA)).thenReturn(false);

    assertThat(validator.isValid(algo, context)).isFalse();
  }

  @Test
  void isValid_ShouldUsePurposeFromAnnotation() {
    // Configure validator for KEY_WRAP
    when(annotation.purpose()).thenReturn(CipherPurpose.KEY_WRAP);
    validator.initialize(annotation);

    String algo = "AES-KW-256";
    when(cryptographyService.isAlgorithmSupported(algo, CipherPurpose.KEY_WRAP)).thenReturn(true);

    assertThat(validator.isValid(algo, context)).isTrue();
  }
}
