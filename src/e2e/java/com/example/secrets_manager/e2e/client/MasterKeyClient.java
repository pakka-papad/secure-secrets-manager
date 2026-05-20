package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;

import com.example.secrets_manager.core.models.MasterKey;
import io.restassured.response.Response;

/** E2E REST client for the Admin Master Key API. */
public class MasterKeyClient {

  private final String token;

  public MasterKeyClient(String token) {
    this.token = token;
  }

  public MasterKey markAsCompromised(int version) {
    return markAsCompromisedRaw(version).then().statusCode(200).extract().as(MasterKey.class);
  }

  public Response markAsCompromisedRaw(int version) {
    return given()
        .header("Authorization", "Bearer " + token)
        .pathParam("version", version)
        .when()
        .post("/api/v1/admin/master-keys/{version}/compromise");
  }
}
