package com.example.secrets_manager.crypto.impl;

import com.example.secrets_manager.crypto.PasswordHasher;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

  private static final String ALGORITHM_NAME = "BCRYPT";

  @Override
  public String getAlgorithmName() {
    return ALGORITHM_NAME;
  }

  @Override
  public HashedPassword hash(byte[] plaintextPassword) {
    // Note: BCrypt works on Strings. We convert from bytes at the last possible moment.
    // The original byte array should be cleared by the caller after this method is called.
    String passwordAsString = new String(plaintextPassword, StandardCharsets.UTF_8);
    String salt = BCrypt.gensalt();
    String digest = BCrypt.hashpw(passwordAsString, salt);

    // BCrypt stores the salt and digest together, so we return the combined string
    // as the digest and an empty salt.
    return new HashedPassword(
        digest.getBytes(StandardCharsets.UTF_8),
        new byte[0],
        ALGORITHM_NAME,
        Collections.emptyMap());
  }

  @Override
  public boolean verify(byte[] plaintextPassword, HashedPassword storedHash) {
    if (!ALGORITHM_NAME.equals(storedHash.getAlgorithm())) {
      throw new IllegalArgumentException("Hash was not created with BCRYPT algorithm");
    }
    // Note: BCrypt works on Strings. We convert from bytes at the last possible moment.
    String passwordAsString = new String(plaintextPassword, StandardCharsets.UTF_8);
    String storedDigest = new String(storedHash.getDigest(), StandardCharsets.UTF_8);
    return BCrypt.checkpw(passwordAsString, storedDigest);
  }
}
