package com.example.secrets_manager.security.forensics;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for extracting forensic details from a failed secured method invocation.
 * Provides class and method name context to ensure unique identification.
 */
public interface MethodSecurityForensicStrategy {

  /** The class containing the secured method. */
  Class<?> getTargetClass();

  /** The name of the method this strategy handles. */
  String getMethodName();

  /**
   * Generates method-specific forensic details as a string.
   *
   * @param invocation The intercepted method call.
   * @return A string containing forensic details, or null if no specific details are provided.
   */
  @Nullable String getForensicDetails(@NonNull MethodInvocation invocation);
}
