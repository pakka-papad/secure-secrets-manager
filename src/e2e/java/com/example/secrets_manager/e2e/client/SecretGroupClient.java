package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.SecretGroupResponse;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;

public class SecretGroupClient {
  private final String token;

  public SecretGroupClient(String token) {
    this.token = token;
  }

  public SecretGroupResponse create(String name, String algorithm) {
    return given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("name", name, "encryptAlgo", algorithm))
        .when()
        .post("/api/v1/secret-groups")
        .then()
        .statusCode(201)
        .body("name", equalTo(name))
        .extract()
        .as(SecretGroupResponse.class);
  }

  public SecretGroupResponse get(UUID id) {
    return given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/v1/secret-groups/" + id)
        .then()
        .statusCode(200)
        .body("id", equalTo(id.toString()))
        .extract()
        .as(SecretGroupResponse.class);
  }

  public PagedResponse<SecretGroupResponse> list(Map<String, ?> params) {
    return given()
        .header("Authorization", "Bearer " + token)
        .params(params)
        .when()
        .get("/api/v1/secret-groups")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<PagedResponse<SecretGroupResponse>>() {});
  }

  public void delete(UUID id) {
    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .delete("/api/v1/secret-groups/" + id)
        .then()
        .statusCode(204);
  }
}
