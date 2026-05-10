package com.example.secrets_manager.core.components;

import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.core.models.search.MasterKeySearchCriteria;
import com.example.secrets_manager.core.services.InternalMasterKeyService;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.security.SecurityUtils;
import com.example.secrets_manager.tracing.CorrelationContext;
import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

/**
 * High-privilege component responsible for loading Master Keys from the Spring Environment.
 *
 * <p>Directly scans all active PropertySources for the pattern MASTER_KEY__V{n}. This provides the
 * most robust support for POSIX environment variables while maintaining compatibility with Spring's
 * DynamicPropertyRegistry used in E2E tests.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MasterKeyProvider {

  private final Map<Integer, byte[]> masterKeys = new ConcurrentHashMap<>();
  private final CryptographyService cryptographyService;
  private final InternalMasterKeyService internalMasterKeyService;
  private final ConfigurableEnvironment environment;

  @Value("${MASTER_KEY_DEFAULT_ALGORITHM:AES-256-GCM}")
  private String defaultAlgorithm;

  // Case-insensitive pattern to match keys like "MASTER_KEY__V1"
  private static final Pattern MASTER_KEY_PATTERN = Pattern.compile("(?i)MASTER_KEY__V(\\d+)");

  @PostConstruct
  public void init() {
    // Generate a fresh UUIDv7 for this specific initialization run
    final var masterKeyInitId = UuidVersion7Strategy.INSTANCE.generateUuid(null);
    CorrelationContext.runWithId(masterKeyInitId, this::performInit);
  }

  private void performInit() {
    // Fetch entire DB registry once to build a lookup map
    final var requiredKeys =
        internalMasterKeyService.listMasterKeys(
            MasterKeySearchCriteria.builder()
                .statuses(EnumSet.of(MasterKeyState.ACTIVE, MasterKeyState.RETIRED))
                .build());

    final var dbKeyMap =
        requiredKeys.stream().collect(Collectors.toMap(MasterKey::getVersion, Function.identity()));

    int currentMaxDbVersion = internalMasterKeyService.getHighestMasterKeyVersion();

    // Discover matching keys across all Spring Property Sources
    Map<Integer, String> discoveredKeys = scanForMasterKeys();

    Integer highestNewVersion = null;
    byte[] highestNewKeyBytes = null;

    // Process discovered keys
    for (Map.Entry<Integer, String> entry : discoveredKeys.entrySet()) {
      int version = entry.getKey();
      String base64Value = entry.getValue();
      MasterKey dbMeta = dbKeyMap.get(version);

      if (version > currentMaxDbVersion) {
        if (highestNewVersion == null || version > highestNewVersion) {
          highestNewVersion = version;
          highestNewKeyBytes = decodeAndValidate(version, base64Value, defaultAlgorithm);
        }
      } else if (dbMeta != null) {
        byte[] bytes = decodeAndValidate(version, base64Value, dbMeta.getEncryptAlgo());
        this.masterKeys.put(version, bytes);
      }
    }

    // Validation: Ensure all ACTIVE/RETIRED keys from DB are now in memory
    for (var dbKey : requiredKeys) {
      if (!this.masterKeys.containsKey(dbKey.getVersion())) {
        throw new IllegalStateException(
            String.format(
                "FATAL: Required Master Key v%d is missing from environment (Expected: MASTER_KEY__V%d).",
                dbKey.getVersion(), dbKey.getVersion()));
      }
    }

    // Atomic Promotion (if new key found)
    if (highestNewVersion != null) {
      // Ensure system security context for the service call
      final var versionToPromote = highestNewVersion;
      final var algorithmToUse = defaultAlgorithm;
      SecurityUtils.runAsSystem(
          () -> internalMasterKeyService.promoteNewKeyInternal(versionToPromote, algorithmToUse));
      this.masterKeys.put(highestNewVersion, highestNewKeyBytes);
    }

    if (this.masterKeys.isEmpty()) {
      throw new IllegalStateException("FATAL: No valid master keys found.");
    }

    log.info(
        "MasterKeyProvider initialized with {} keys. Active version: {}",
        masterKeys.size(),
        getActiveVersion());
  }

  /** Scans all property sources in the environment for keys matching the MASTER_KEY pattern. */
  private Map<Integer, String> scanForMasterKeys() {
    Map<Integer, String> result = new HashMap<>();
    for (PropertySource<?> source : environment.getPropertySources()) {
      if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
        continue;
      }
      for (String propertyName : enumerable.getPropertyNames()) {
        Matcher matcher = MASTER_KEY_PATTERN.matcher(propertyName);
        if (!matcher.matches()) {
          continue;
        }
        String value = environment.getProperty(propertyName);
        if (StringUtils.isBlank(value)) {
          continue;
        }
        result.put(Integer.parseInt(matcher.group(1)), value);
      }
    }
    return result;
  }

  private byte[] decodeAndValidate(int version, String base64Value, String algorithm) {
    try {
      byte[] bytes = Base64.getDecoder().decode(base64Value.trim());
      int requiredLength = cryptographyService.getRequiredSymmetricKeySizeBytes(algorithm);

      if (bytes.length != requiredLength) {
        throw new IllegalStateException(
            String.format(
                "Master Key v%d has invalid length. Expected %d bytes for %s, got %d.",
                version, requiredLength, algorithm, bytes.length));
      }
      return bytes;
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Invalid Base64 for Master Key v" + version, e);
    }
  }

  public byte[] getMasterKey(Integer version) {
    byte[] key = masterKeys.get(version);
    if (key == null) {
      throw new IllegalArgumentException(
          String.format(
              "Master key v%d is not available in memory. It may be compromised, inactive, or missing from environment.",
              version));
    }
    return key;
  }

  /** Returns the version of the currently ACTIVE master key. */
  public Integer getActiveVersion() {
    return masterKeys.keySet().stream()
        .max(Integer::compareTo)
        .orElseThrow(() -> new IllegalStateException("No active master key available."));
  }
}
