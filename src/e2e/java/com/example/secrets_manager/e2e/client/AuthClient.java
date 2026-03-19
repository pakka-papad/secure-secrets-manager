package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;

public class AuthClient {
  
  public String login(String username, String password) {
    return given()
        .contentType(ContentType.URLENC)
        .formParam("username", username)
        .formParam("password", password)
        .when()
        .post("/api/v1/auth/login")
        .then()
        .statusCode(200)
        .body("access_token", notNullValue())
        .extract()
        .path("access_token");
  }
}
