package com.example.secrets_manager.core.components;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * High-privilege component responsible for loading Master Keys from the operating system
 * environment. Strictly enforces OS Environment Variables as the single source of truth for
 * cryptographic root keys.
 */
@Component
@Slf4j
public class MasterKeyProvider {

  private final Map<Integer, byte[]> masterKeys = new ConcurrentHashMap<>();

  // Case-insensitive pattern to match environment variables like "master_key__v1"
  private static final Pattern MASTER_KEY_PATTERN = Pattern.compile("(?i)master_key__v(\\d+)");

  @PostConstruct
  public void init() {
    // Strictly iterate only the OS Environment Variables
    System.getenv().forEach(this::processEnvVar);

    if (masterKeys.isEmpty()) {
      throw new IllegalStateException(
          "FATAL: No master keys were found in OS environment variables (expected format: 'master_key__v1', 'master_key__v2', etc.). "
              + "Ensure keys are exported to the process environment.");
    }

    log.info("MasterKeyProvider initialized with {} keys from the environment.", masterKeys.size());
  }

  private void processEnvVar(String key, String value) {
    if (StringUtils.isBlank(value)) {
      return;
    }

    final var matcher = MASTER_KEY_PATTERN.matcher(key);
    if (!matcher.matches()) {
      return;
    }

    try {
      int version = Integer.parseInt(matcher.group(1));

      // Trim to handle accidental whitespace from environment injection/scripts
      byte[] masterKey = Base64.getDecoder().decode(value.trim());

      // Validate key length (AES-256 requires 32 bytes)
      if (masterKey.length != 32) {
        log.error(
            "Invalid master key length for '{}'. Expected 32 bytes, got {}.",
            key,
            masterKey.length);
        throw new IllegalStateException(
            String.format("Master key '%s' must be 32 bytes (256 bits) for AES-256.", key));
      }

      masterKeys.put(version, masterKey);
      log.info(
          "Successfully loaded Master Key Version: {} from environment variable: {}", version, key);

    } catch (NumberFormatException e) {
      log.warn("Invalid version number in environment variable key: {}. Skipping.", key);
    } catch (IllegalArgumentException e) {
      log.warn(
          "Invalid Base64 encoding for master key in environment variable '{}'. Skipping.", key);
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
