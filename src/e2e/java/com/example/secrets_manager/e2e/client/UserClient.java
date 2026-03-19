package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.example.secrets_manager.api.rest.dto.UserResponse;
import io.restassured.http.ContentType;
import java.util.Map;

public class UserClient {
  private final String token;

  public UserClient(String token) {
    this.token = token;
  }

  public UserResponse create(String username, String password) {
    return given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(Map.of("name", username, "password", password))
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
}
