package com.example.secrets_manager.core.validators;

import com.example.secrets_manager.crypto.CipherPurpose;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EncryptionAlgorithmValidator.class)
public @interface EncryptionAlgorithm {
  String message() default "Invalid or unauthorized encryption algorithm";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  CipherPurpose purpose() default CipherPurpose.DATA;
}
