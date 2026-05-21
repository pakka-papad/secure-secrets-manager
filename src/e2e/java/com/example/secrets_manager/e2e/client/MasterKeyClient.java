package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;

import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.core.models.MasterKey;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.util.Map;

/** E2E REST client for the Admin Master Key API. */
public class MasterKeyClient {

  private final String token;

  public MasterKeyClient(String token) {
    this.token = token;
  }

  public PagedResponse<MasterKey> list(Map<String, ?> queryParams) {
    return given()
        .header("Authorization", "Bearer " + token)
        .queryParams(queryParams)
        .when()
        .get("/api/v1/admin/master-keys")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<>() {});
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
