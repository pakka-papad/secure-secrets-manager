package com.example.secrets_manager.core.components;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MasterKeyProvider {

  private final Map<Integer, byte[]> masterKeys = new ConcurrentHashMap<>();

  // Pattern to match environment variables like "master_key__v1", "master_key__v2", etc.
  private static final Pattern MASTER_KEY_ENV_PATTERN = Pattern.compile("master_key__v(\\d+)");

  @PostConstruct
  public void init() {
    System.getenv().forEach(this::processEnvVar);

    if (masterKeys.isEmpty()) {
      throw new IllegalStateException(
          "FATAL: No master keys were found in environment variables (expected format: 'master_key__v1', 'master_key__v2', etc.). Application cannot start.");
    }
  }

  private void processEnvVar(String envKey, String envValue) {
    Matcher matcher = MASTER_KEY_ENV_PATTERN.matcher(envKey);
    if (!matcher.matches()) {
      return;
    }
    try {
      int version = Integer.parseInt(matcher.group(1)); // Extract version number
      byte[] masterKey = Base64.getDecoder().decode(envValue); // Decode Base64 value

      // Validate key length (e.g., for AES-256, it should be 32 bytes)
      if (masterKey.length != 32) {
        throw new IllegalStateException(
            String.format("Master key '%s' must be 32 bytes (256 bits) for AES-256.", envKey));
      }
      masterKeys.put(version, masterKey);
      log.info("Loaded Master Key Version: {}", version);
    } catch (NumberFormatException e) {
      log.error("Warning: Invalid version number in environment variable: {}. Skipping.", envKey);
    } catch (IllegalArgumentException e) {
      log.error("Warning: Invalid Base64 encoding for master key '{}'. Skipping.", envKey);
    }
  }

  public byte[] getMasterKey(Integer version) {
    byte[] key = masterKeys.get(version);
    if (key == null) {
      throw new IllegalArgumentException(
          String.format("Master key not found for version: %d", version));
    }
    return key;
  }
}
