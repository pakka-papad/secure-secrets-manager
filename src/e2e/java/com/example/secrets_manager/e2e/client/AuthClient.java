package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import com.example.secrets_manager.core.models.AuthResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;

public class AuthClient {

  public String login(String username, String password) {
    return loginExtended(username, password).getAccessToken();
  }

  public AuthResponse loginExtended(String username, String password) {
    return loginRaw(username, password)
        .then()
        .statusCode(200)
        .body("access_token", notNullValue())
        .extract()
        .as(AuthResponse.class);
  }

  public Response loginRaw(String username, String password) {
    return given()
        .contentType(ContentType.URLENC)
        .formParam("username", username)
        .formParam("password", password)
        .when()
        .post("/api/v1/auth/login");
  }

  public AuthResponse refresh(String refreshToken) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("refreshToken", refreshToken))
        .when()
        .post("/api/v1/auth/refresh")
        .then()
        .statusCode(200)
        .body("access_token", notNullValue())
        .extract()
        .as(AuthResponse.class);
  }
}
