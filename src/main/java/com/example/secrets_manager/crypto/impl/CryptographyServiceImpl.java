package com.example.secrets_manager.crypto.impl;

import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.PasswordHasher;
import com.example.secrets_manager.crypto.SymmetricCipher;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CryptographyServiceImpl implements CryptographyService {

  private final Map<String, PasswordHasher> passwordHashers;
  private final Map<String, SymmetricCipher> symmetricCiphers;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  public CryptographyServiceImpl(
      List<PasswordHasher> passwordHashers, List<SymmetricCipher> symmetricCiphers) {
    this.passwordHashers =
        passwordHashers.stream()
            .collect(Collectors.toMap(PasswordHasher::getAlgorithmName, Function.identity()));
    this.symmetricCiphers =
        symmetricCiphers.stream()
            .collect(Collectors.toMap(SymmetricCipher::getAlgorithmName, Function.identity()));
  }

  @Override
  public HashedPassword hashPassword(byte[] plaintextPassword) {
    // Use BCRYPT as the default password hasher for new hashes
    PasswordHasher hasher = passwordHashers.get("BCRYPT");
    if (hasher == null) {
      throw new IllegalStateException(
          "BCRYPT password hasher not found. Check crypto configuration.");
    }
    return hasher.hash(plaintextPassword);
  }

  @Override
  public boolean verifyPassword(byte[] plaintextPassword, HashedPassword storedHash) {
    PasswordHasher hasher = passwordHashers.get(storedHash.getAlgorithm());
    if (hasher == null) {
      throw new IllegalArgumentException(
          "Unsupported password hashing algorithm: " + storedHash.getAlgorithm());
    }
    return hasher.verify(plaintextPassword, storedHash);
  }

  @Override
  public EncryptedData encrypt(byte[] plaintext, byte[] key, String algorithmName) {
    SymmetricCipher cipher = symmetricCiphers.get(algorithmName);
    if (cipher == null) {
      throw new IllegalArgumentException(
          "Unsupported symmetric cipher algorithm: " + algorithmName);
    }
    return cipher.encrypt(plaintext, key);
  }

  @Override
  public byte[] decrypt(EncryptedData data, byte[] key) throws CryptoOperationException {
    SymmetricCipher cipher = symmetricCiphers.get(data.getAlgorithm());
    if (cipher == null) {
      throw new IllegalArgumentException(
          "Unsupported symmetric cipher algorithm: ".concat(data.getAlgorithm()));
    }
    return cipher.decrypt(data, key);
  }

  @Override
  public byte[] createDataHash(Object dataToHash) {
    try {
      // Use a stable JSON representation for hashing to ensure determinism.
      // ObjectMapper is suitable for this, ensuring consistent ordering of fields.
      byte[] serializedData = objectMapper.writeValueAsBytes(dataToHash);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(serializedData);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      throw new RuntimeException("Could not create data hash", e);
    }
  }
}
