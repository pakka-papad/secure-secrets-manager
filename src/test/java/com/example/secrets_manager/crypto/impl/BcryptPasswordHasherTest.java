package com.example.secrets_manager.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.crypto.dto.HashedPassword;
import org.junit.jupiter.api.Test;

class BcryptPasswordHasherTest {

  private final BcryptPasswordHasher hasher = new BcryptPasswordHasher();

  @Test
  void hash_ShouldProduceValidHash() {
    // Given
    byte[] password = "securePassword123".getBytes();

    // When
    HashedPassword result = hasher.hash(password);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getAlgorithm()).isEqualTo(hasher.getAlgorithmName());
    assertThat(result.getDigest()).isNotEmpty();
    assertThat(result.getSalt()).isEmpty(); // BCrypt embeds salt in digest
    assertThat(result.getParams()).isEmpty();
  }

  @Test
  void verify_ShouldReturnTrueForCorrectPassword() {
    // Given
    byte[] password = "securePassword123".getBytes();

    // When
    HashedPassword hashed = hasher.hash(password);

    // Then
    boolean matches = hasher.verify(password, hashed);
    assertThat(matches).isTrue();
  }

  @Test
  void verify_ShouldReturnFalseForIncorrectPassword() {
    // Given
    byte[] password = "correctPassword".getBytes();
    byte[] wrongPassword = "wrongPassword".getBytes();

    // When
    HashedPassword hashed = hasher.hash(password);

    // Then
    boolean matches = hasher.verify(wrongPassword, hashed);
    assertThat(matches).isFalse();
  }
}
