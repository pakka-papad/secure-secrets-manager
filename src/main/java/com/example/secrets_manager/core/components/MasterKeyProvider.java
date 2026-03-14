package com.example.secrets_manager.core.components;

import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.core.models.search.MasterKeySearchCriteria;
import com.example.secrets_manager.core.services.InternalMasterKeyService;
import com.example.secrets_manager.crypto.CryptographyService;
import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * High-privilege component responsible for loading Master Keys from the operating system
 * environment and synchronizing them with the database registry.
 *
 * <p>Optimized to surgically load only necessary cryptographic material into memory.
 */
@Component
@Slf4j
public class MasterKeyProvider {

  private final Map<Integer, byte[]> masterKeys = new ConcurrentHashMap<>();
  private final CryptographyService cryptographyService;
  private final InternalMasterKeyService internalMasterKeyService;
  private final EnvironmentProvider environmentProvider;

  @Value("${master-key.default-algorithm:AES-256-GCM}")
  private String defaultAlgorithm;

  // Case-insensitive pattern to match environment variables like "master_key__v1"
  private static final Pattern MASTER_KEY_PATTERN = Pattern.compile("(?i)master_key__v(\\d+)");

  @Autowired
  public MasterKeyProvider(
      CryptographyService cryptographyService,
      InternalMasterKeyService internalMasterKeyService,
      EnvironmentProvider environmentProvider) {
    this.cryptographyService = cryptographyService;
    this.internalMasterKeyService = internalMasterKeyService;
    this.environmentProvider = environmentProvider;
  }

  @PostConstruct
  public void init() {
    // 1. Fetch entire DB registry once to build a lookup map
    final var requiredKeys =
        internalMasterKeyService.listMasterKeys(
            MasterKeySearchCriteria.builder()
                .statuses(EnumSet.of(MasterKeyState.ACTIVE, MasterKeyState.RETIRED))
                .build());

    final var dbKeyMap =
        requiredKeys.stream().collect(Collectors.toMap(MasterKey::getVersion, Function.identity()));

    int currentMaxDbVersion = internalMasterKeyService.getHighestMasterKeyVersion();

    // 2. Surgical ENV Scan
    Integer highestNewVersion = null;
    byte[] highestNewKeyBytes = null;

    for (Map.Entry<String, String> entry : environmentProvider.getEnvironment().entrySet()) {
      Matcher matcher = MASTER_KEY_PATTERN.matcher(entry.getKey());
      if (!matcher.matches() || StringUtils.isBlank(entry.getValue())) {
        continue;
      }

      int version = Integer.parseInt(matcher.group(1));
      MasterKey dbMeta = dbKeyMap.get(version);

      if (version > currentMaxDbVersion) {
        if (highestNewVersion == null || version > highestNewVersion) {
          highestNewVersion = version;
          highestNewKeyBytes = decodeAndValidate(version, entry.getValue(), defaultAlgorithm);
        }
      } else if (dbMeta != null) {
        byte[] bytes = decodeAndValidate(version, entry.getValue(), dbMeta.getEncryptAlgo());
        this.masterKeys.put(version, bytes);
      }
    }

    // 3. Validation: Ensure all ACTIVE/RETIRED keys from DB are now in memory
    for (var dbKey : requiredKeys) {
      if (!this.masterKeys.containsKey(dbKey.getVersion())) {
        throw new IllegalStateException(
            String.format(
                "FATAL: Required Master Key v%d is missing from ENV.", dbKey.getVersion()));
      }
    }

    // 4. Atomic Promotion (if new key found)
    if (highestNewVersion != null) {
      internalMasterKeyService.promoteNewKeyInternal(highestNewVersion, defaultAlgorithm);
      this.masterKeys.put(highestNewVersion, highestNewKeyBytes);
    }

    if (this.masterKeys.isEmpty()) {
      throw new IllegalStateException("FATAL: No valid master keys found. System cannot operate.");
    }

    log.info(
        "MasterKeyProvider initialized with {} keys. Active version: {}",
        masterKeys.size(),
        getActiveVersion());
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
              "Master key v%d is not available in memory. It may be compromised, inactive, or missing from ENV.",
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
