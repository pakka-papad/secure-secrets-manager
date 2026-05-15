package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;

/** E2E REST client for the Admin Test API (Triggering system events). */
public class AdminTestClient {

  private final String token;

  public AdminTestClient(String token) {
    this.token = token;
  }

  public void triggerMasterKeyPromotion(int version) {
    given()
        .header("Authorization", "Bearer " + token)
        .pathParam("version", version)
        .when()
        .post("/api/v1/admin/test/promote-master-key/{version}")
        .then()
        .statusCode(202);
  }
}
