package com.example.secrets_manager.security.forensics;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registry for method-level security forensic strategies. Resolves exact Method objects at startup
 * to ensure 100% precision and avoid name collisions.
 */
@Component
@Slf4j
public class MethodSecurityForensicRegistry {

  private final Map<Method, MethodSecurityForensicStrategy> strategyMap;

  @Autowired
  public MethodSecurityForensicRegistry(List<MethodSecurityForensicStrategy> strategies) {
    Map<Method, MethodSecurityForensicStrategy> tempMap = new HashMap<>();

    for (var strategy : strategies) {
      try {
        // Resolve the specific method on the target class using reflection.
        // This handles collisions by making the key the actual Method object (Class + Name +
        // Params).
        // NOTE: This currently matches only by name and will pick the first occurrence in case of
        // overloaded methods. To support overloads, the strategy would need to provide parameter
        // types.
        Method resolvedMethod =
            Arrays.stream(strategy.getTargetClass().getDeclaredMethods())
                .filter(m -> m.getName().equals(strategy.getMethodName()))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(strategy.getMethodName()));

        tempMap.put(resolvedMethod, strategy);
        log.info(
            "Registered forensic strategy for method: {}#{}",
            strategy.getTargetClass().getSimpleName(),
            strategy.getMethodName());

      } catch (NoSuchMethodException e) {
        log.error(
            "Failed to register forensic strategy: Method '{}' not found on class '{}'",
            strategy.getMethodName(),
            strategy.getTargetClass().getName());
      }
    }

    this.strategyMap = Collections.unmodifiableMap(tempMap);
  }

  /**
   * Retrieves the appropriate forensic strategy for a given method.
   *
   * @param method The exact method that failed authorization.
   * @return An Optional containing the strategy if found, otherwise empty.
   */
  public Optional<MethodSecurityForensicStrategy> getStrategy(Method method) {
    return Optional.ofNullable(strategyMap.get(method));
  }
}
