package com.example.secrets_manager.e2e.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.api.rest.dto.UserResponse;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AdminUserManagementE2ETest extends E2EBaseTest {

  @Test
  void shouldPerformComplexAdminUserManagement() {
    // 1. Get ANY admin
    var initAdmin = actors.asAnyAdmin();
    String newAdminName = "sub-admin-" + UUID.randomUUID();
    String password = "Password1234!";
    UserResponse subAdmin =
        initAdmin.users().create(newAdminName, password, Set.of("ADMIN", "USER"));

    // 2. New admin logs in
    var admin = actors.asUser(newAdminName, password);

    // 3. New admin creates 50 users in parallel (Limited to 10 threads)
    List<UserResponse> createdUsers = Collections.synchronizedList(new ArrayList<>());
    ExecutorService executor = Executors.newFixedThreadPool(10);

    try {
      List<CompletableFuture<Void>> futures =
          IntStream.rangeClosed(1, 50)
              .mapToObj(
                  i ->
                      CompletableFuture.runAsync(
                          () -> {
                            String uname = "bulk-user-" + i + "-" + UUID.randomUUID();
                            createdUsers.add(admin.users().create(uname, password));
                          },
                          executor))
              .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    } finally {
      executor.shutdown();
    }

    assertThat(createdUsers).hasSize(50);

    // 4. Try creating one more user with an existing name (Conflict expected)
    String existingName = createdUsers.get(0).getName();
    admin.users().createRaw(existingName, password, Set.of("USER")).then().statusCode(409);

    // 5. Pick a random user and delete it (not self)
    UserResponse randomUser = createdUsers.get(new Random().nextInt(createdUsers.size()));
    admin.users().delete(randomUser.getId());

    // 6. Verify deleted user is gone from listings
    var afterDeleteList = admin.users().list(Map.of("size", "100"));
    assertThat(afterDeleteList.getItems()).noneMatch(u -> u.getId().equals(randomUser.getId()));

    // 7. Delete self (Expect 403 Forbidden - Self Deletion Guardrail)
    admin.users().deleteRaw(subAdmin.getId()).then().statusCode(403);

    // 8. Delete the original admin (Expect 204 - Admin can delete other Admin)
    var allUsers = admin.users().list(Map.of("size", "100", "name", initAdmin.getUsername()));
    UUID initAdminId =
        allUsers.getItems().stream()
            .filter(u -> u.getName().equals(BOOTSTRAP_ADMIN_USERNAME))
            .findFirst()
            .map(UserResponse::getId)
            .orElseThrow();

    admin.users().delete(initAdminId);

    // 9. Verify initial admin is gone
    var finalCheck = admin.users().list(Map.of("size", "100", "name", initAdmin.getUsername()));
    assertThat(finalCheck.getItems()).noneMatch(u -> u.getName().equals(initAdmin.getUsername()));
  }
}
