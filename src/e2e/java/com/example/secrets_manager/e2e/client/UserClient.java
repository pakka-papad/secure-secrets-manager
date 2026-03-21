package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.UserResponse;
import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.e2e.actor.ActorRegistry;
import com.example.secrets_manager.e2e.actor.UserCredential;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.*;

public class UserClient {
  private final String token;

  public UserClient(String token) {
    this.token = token;
  }

  public UserResponse create(String username, String password) {
    return create(username, password, Set.of("USER"));
  }

  public UserResponse create(String username, String password, Set<String> roles) {
    return createRaw(username, password, roles)
        .then()
        .statusCode(201)
        .body("name", equalTo(username))
        .body("id", notNullValue())
        .extract()
        .as(UserResponse.class);
  }

  public Response createRaw(String username, String password, Set<String> roles) {
    final var response =
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", username, "password", password, "roles", roles))
            .when()
            .post("/api/v1/users");
    if (response.statusCode() == 201) {
      final var typedResponse = response.as(UserResponse.class);
      ActorRegistry.register(
          new UserCredential(typedResponse.getId(), username, password, typedResponse.getRoles()));
    }
    return response;
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

  public UserResponse get(UUID userId) {
    return getRaw(userId).then().statusCode(200).extract().as(UserResponse.class);
  }

  public Response getRaw(UUID userId) {
    return given().header("Authorization", "Bearer " + token).when().get("/api/v1/users/" + userId);
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
        .as(new TypeRef<>() {
        });
  }

  public void delete(UUID userId) {
    deleteRaw(userId).then().statusCode(204);
  }

  public Response deleteRaw(UUID userId) {
    final var response =
        given().header("Authorization", "Bearer " + token).when().delete("/api/v1/users/" + userId);
    if (response.statusCode() == 204) {
      ActorRegistry.unregister(userId);
    }
    return response;
  }

  public UserResponse updateRoles(UUID userId, Set<UserRole> roles) {
    return updateRolesRaw(userId, roles).then().statusCode(200).extract().as(UserResponse.class);
  }

  public Response updateRolesRaw(UUID userId, Set<UserRole> roles) {
    final var response =
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("roles", roles))
            .when()
            .put("/api/v1/users/" + userId + "/roles");
    if (response.statusCode() == 200) {
      final var typedResponse = response.as(UserResponse.class);
      ActorRegistry.updateRoles(userId, typedResponse.getRoles());
    }
    return response;
  }

  public UserResponse updatePassword(UUID userId, String oldPassword, String newPassword) {
    return updatePasswordRaw(userId, oldPassword, newPassword)
        .then()
        .statusCode(200)
        .extract()
        .as(UserResponse.class);
  }

  public Response updatePasswordRaw(UUID userId, String oldPassword, String newPassword) {
    final var response =
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("oldPassword", oldPassword, "newPassword", newPassword))
            .when()
            .put("/api/v1/users/me/password");
    if (response.statusCode() == 200) {
      ActorRegistry.updatePassword(userId, newPassword);
    }
    return response;
  }
}
