package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;

import com.example.secrets_manager.api.rest.dto.ModifyAuthorizationRequest;
import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationDetailedResponse;
import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationResponse;
import com.example.secrets_manager.core.models.PermissionType;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SecretGroupAuthorizationClient {
  private final String token;

  public SecretGroupAuthorizationClient(String token) {
    this.token = token;
  }

  public SecretGroupAuthorizationResponse update(
      UUID groupId, UUID userId, Set<PermissionType> permissions) {
    return updateRaw(groupId, userId, permissions)
        .then()
        .statusCode(200)
        .extract()
        .as(SecretGroupAuthorizationResponse.class);
  }

  public void revoke(UUID groupId, UUID userId) {
    updateRaw(groupId, userId, Set.of()).then().statusCode(204);
  }

  public Response updateRaw(UUID groupId, UUID userId, Set<PermissionType> permissions) {
    var request = new ModifyAuthorizationRequest(permissions);
    return given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/v1/secret-groups/" + groupId + "/authorizations/" + userId);
  }

  public PagedResponse<SecretGroupAuthorizationDetailedResponse> list(
      UUID groupId, Map<String, ?> params) {
    return given()
        .header("Authorization", "Bearer " + token)
        .params(params)
        .when()
        .get("/api/v1/secret-groups/" + groupId + "/authorizations")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<PagedResponse<SecretGroupAuthorizationDetailedResponse>>() {});
  }

  public SecretGroupAuthorizationDetailedResponse get(UUID groupId, UUID userId) {
    return getRaw(groupId, userId)
        .then()
        .statusCode(200)
        .extract()
        .as(SecretGroupAuthorizationDetailedResponse.class);
  }

  public Response getRaw(UUID groupId, UUID userId) {
    return given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/v1/secret-groups/" + groupId + "/authorizations/" + userId);
  }

  public Response getRaw(UUID groupId) {
    return given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/v1/secret-groups/" + groupId + "/authorizations");
  }
}
