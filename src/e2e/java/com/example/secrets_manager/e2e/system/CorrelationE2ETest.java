package com.example.secrets_manager.e2e.system;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.e2e.base.E2EBaseTest;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CorrelationE2ETest extends E2EBaseTest {

  @Test
  void concurrentRequests_ShouldHaveUniqueCorrelationIds() throws InterruptedException {
    final var admin = actors.asAnyAdmin();
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    Set<String> correlationIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                try {
                  var response =
                      given()
                          .header("Authorization", "Bearer " + admin.getToken())
                          .when()
                          .get("/api/v1/system/algorithms/symmetric");

                  String cid = response.getHeader("X-Correlation-ID");
                  if (cid != null) {
                    correlationIds.add(cid);
                  }
                } finally {
                  latch.countDown();
                }
              })
          .start();
    }

    latch.await(10, TimeUnit.SECONDS);

    // Verify all requests got a unique ID
    assertThat(correlationIds).hasSize(threadCount);
  }
}
