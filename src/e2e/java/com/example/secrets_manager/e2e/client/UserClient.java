package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.UserResponse;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UserClient {
  private final String token;

  public UserClient(String token) {
    this.token = token;
  }

  public UserResponse create(String username, String password) {
    return create(username, password, Set.of("USER"));
  }

  public UserResponse create(String username, String password, Set<String> roles) {
    return given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("name", username, "password", password, "roles", roles))
        .when()
        .post("/api/v1/users")
        .then()
        .statusCode(201)
        .body("name", equalTo(username))
        .body("id", notNullValue())
        .extract()
        .as(UserResponse.class);
  }

  public UserResponse me() {
    return given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .when()
        .get("/api/v1/users/me")
        .then()
        .statusCode(200)
        .extract()
        .as(UserResponse.class);
  }

  public PagedResponse<UserResponse> list(Map<String, ?> params) {
    return given()
        .header("Authorization", "Bearer " + token)
        .params(params)
        .when()
        .get("/api/v1/users")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<PagedResponse<UserResponse>>() {});
  }

  public void delete(UUID userId) {
    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .delete("/api/v1/users/" + userId)
        .then()
        .statusCode(204);
  }

  public UserResponse updateRoles(UUID userId, Set<String> roles) {
    return given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("roles", roles))
        .when()
        .put("/api/v1/users/" + userId + "/roles")
        .then()
        .statusCode(200)
        .extract()
        .as(UserResponse.class);
  }
}
