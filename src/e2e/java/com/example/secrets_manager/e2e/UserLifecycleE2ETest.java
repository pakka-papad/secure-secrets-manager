package com.example.secrets_manager.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserLifecycleE2ETest extends E2EBaseTest {

  @Test
  void shouldPerformFullUserLifecycle() {
    String username = "e2eUser" + System.currentTimeMillis();
    String password = "Password1234";

    // 1. Login as Bootstrap Admin (Form Params)
    String adminToken =
        given()
            .port(RestAssured.port)
            .contentType(ContentType.URLENC)
            .formParam("username", BOOTSTRAP_ADMIN_USERNAME)
            .formParam("password", BOOTSTRAP_ADMIN_PASSWORD)
            .when()
            .post("/api/v1/auth/login")
            .then()
            .statusCode(200)
            .body("access_token", notNullValue())
            .extract()
            .path("access_token");

    // 2. Create a new user using Admin Token (JSON Body)
    given(requestSpec)
        .header("Authorization", "Bearer " + adminToken)
        .body(Map.of("name", username, "password", password))
        .when()
        .post("/api/v1/users")
        .then()
        .statusCode(201)
        .body("name", equalTo(username))
        .body("id", notNullValue());

    // 3. Login as the newly created user (Form Params)
    String userToken =
        given()
            .port(RestAssured.port)
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

    // 4. Access 'me' endpoint with the user token
    withAuth(userToken)
        .when()
        .get("/api/v1/users/me")
        .then()
        .statusCode(200)
        .body("name", equalTo(username))
        .body("roles", hasItem("USER"));
  }
}
