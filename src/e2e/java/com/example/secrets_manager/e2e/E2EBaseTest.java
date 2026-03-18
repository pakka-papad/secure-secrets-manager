package com.example.secrets_manager.e2e;

import com.example.secrets_manager.core.components.EnvironmentProvider;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(E2EBaseTest.TestEnvConfig.class)
public abstract class E2EBaseTest {

  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18").withEnv("TZ", "UTC");

  private static final String JWT_PUBLIC_KEY;
  private static final String JWT_PRIVATE_KEY;
  private static final String MASTER_KEY__V1;

  static {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    postgres.start();
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
      keyPairGenerator.initialize(256);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      JWT_PUBLIC_KEY = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
      JWT_PRIVATE_KEY = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

      byte[] mk = new byte[32]; // AES-256
      new java.security.SecureRandom().nextBytes(mk);
      MASTER_KEY__V1 = Base64.getEncoder().encodeToString(mk);
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize E2E test secrets", e);
    }
  }

  @TestConfiguration
  static class TestEnvConfig {
    @Bean
    @Primary
    public EnvironmentProvider testEnvironmentProvider() {
      return () -> {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("MASTER_KEY__V1", MASTER_KEY__V1);
        return env;
      };
    }
  }

  protected static final String BOOTSTRAP_ADMIN_USERNAME = "e2eBootAdmin";
  protected static final String BOOTSTRAP_ADMIN_PASSWORD = "AdminPassword1234";

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // Directly override all connection properties for both Spring Datasource and Flyway
    // This ensures no placeholders from the main application.yml interfere
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

    registry.add("spring.flyway.url", postgres::getJdbcUrl);
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);

    // Real generated keys for E2E
    registry.add("JWT_PUBLIC_KEY", () -> JWT_PUBLIC_KEY);
    registry.add("JWT_PRIVATE_KEY", () -> JWT_PRIVATE_KEY);

    registry.add("MASTER_KEY__V1", () -> MASTER_KEY__V1);
    registry.add("MASTER_KEY_DEFAULT_ALGORITHM", () -> "AES-256-GCM");

    registry.add("BOOTSTRAP_USERNAME", () -> BOOTSTRAP_ADMIN_USERNAME);
    registry.add("BOOTSTRAP_PASSWORD", () -> BOOTSTRAP_ADMIN_PASSWORD);
  }

  @LocalServerPort private int port;

  protected RequestSpecification requestSpec;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    requestSpec =
        new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .build();
  }

  protected RequestSpecification withAuth(String token) {
    return RestAssured.given(requestSpec).header("Authorization", "Bearer " + token);
  }
}
