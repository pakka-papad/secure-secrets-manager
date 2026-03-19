package com.example.secrets_manager.e2e.base;

import com.example.secrets_manager.e2e.actor.ActorFactory;
import io.restassured.RestAssured;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class E2EBaseTest {

  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18").withEnv("TZ", "UTC");

  protected static final String BOOTSTRAP_ADMIN_USERNAME = "e2eBootAdmin";
  protected static final String BOOTSTRAP_ADMIN_PASSWORD = "AdminPassword1234";

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

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // 1. Database
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

    registry.add("spring.flyway.url", postgres::getJdbcUrl);
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);

    // 2. JWT Keys
    registry.add("jwt.secret-key.public", () -> JWT_PUBLIC_KEY);
    registry.add("jwt.secret-key.private", () -> JWT_PRIVATE_KEY);

    // 3. Master Key
    registry.add("MASTER_KEY__V1", () -> MASTER_KEY__V1);
    registry.add("MASTER_KEY_DEFAULT_ALGORITHM", () -> "AES-256-GCM");

    // 4. Bootstrap Credentials
    registry.add("bootstrap.admin.username", () -> BOOTSTRAP_ADMIN_USERNAME);
    registry.add("bootstrap.admin.password", () -> BOOTSTRAP_ADMIN_PASSWORD);
  }

  @LocalServerPort private int port;

  protected ActorFactory actors;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    actors = new ActorFactory(BOOTSTRAP_ADMIN_USERNAME, BOOTSTRAP_ADMIN_PASSWORD);
  }
}
