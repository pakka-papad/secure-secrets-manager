package com.example.secrets_manager.core.components;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Standard implementation that provides access to the real OS environment variables. */
@Component
public class SystemEnvironmentProvider implements EnvironmentProvider {
  @Override
  public Map<String, String> getEnvironment() {
    return System.getenv();
  }
}
