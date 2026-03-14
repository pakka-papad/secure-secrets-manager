package com.example.secrets_manager.core.components;

import java.util.Map;

/**
 * Strategy interface for providing access to the application's operating environment. Primarily
 * used to abstract System.getenv() for testability.
 */
public interface EnvironmentProvider {
  /** Returns a map of current environment variables. */
  Map<String, String> getEnvironment();
}
