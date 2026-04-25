package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.example.secrets_manager.api.rest.dto.*;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;

public class SecretClient {
  private final String token;

  public SecretClient(String token) {
    this.token = token;
  }

  public SecretMetadataResponse create(UUID groupId, String name, String value) {
    var request = new CreateSecretRequest(name, value);
    return createRaw(groupId, request)
        .then()
        .statusCode(201)
        .body("name", equalTo(name))
        .extract()
        .as(SecretMetadataResponse.class);
  }

  public Response createRaw(UUID groupId, CreateSecretRequest request) {
    return given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/v1/secret-groups/" + groupId + "/secrets");
  }

  public PagedResponse<SecretMetadataResponse> list(UUID groupId, Map<String, ?> params) {
    return listRaw(groupId, params)
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<PagedResponse<SecretMetadataResponse>>() {});
  }

  public Response listRaw(UUID groupId, Map<String, ?> params) {
    return given()
        .header("Authorization", "Bearer " + token)
        .params(params)
        .when()
        .get("/api/v1/secret-groups/" + groupId + "/secrets");
  }

  public SecretMetadataResponse getMetadata(UUID groupId, String name) {
    return given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/v1/secret-groups/" + groupId + "/secrets/" + name)
        .then()
        .statusCode(200)
        .extract()
        .as(SecretMetadataResponse.class);
  }

  public SecretValueResponse getValue(UUID groupId, String name) {
    return getValueRaw(groupId, name)
        .then()
        .statusCode(200)
        .extract()
        .as(SecretValueResponse.class);
  }

  public Response getValueRaw(UUID groupId, String name) {
    return given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/v1/secret-groups/" + groupId + "/secrets/" + name + "/value");
  }

  public SecretMetadataResponse updateValue(UUID groupId, String name, String newValue) {
    var request = new UpdateSecretValueRequest(newValue);
    return updateValueRaw(groupId, name, request)
        .then()
        .statusCode(200)
        .extract()
        .as(SecretMetadataResponse.class);
  }

  public Response updateValueRaw(UUID groupId, String name, UpdateSecretValueRequest request) {
    return given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/v1/secret-groups/" + groupId + "/secrets/" + name + "/value");
  }

  public void delete(UUID groupId, String name) {
    deleteRaw(groupId, name).then().statusCode(204);
  }

  public Response deleteRaw(UUID groupId, String name) {
    return given()
        .header("Authorization", "Bearer " + token)
        .when()
        .delete("/api/v1/secret-groups/" + groupId + "/secrets/" + name);
  }
}
