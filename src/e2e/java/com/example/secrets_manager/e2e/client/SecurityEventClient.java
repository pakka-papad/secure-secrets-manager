package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;

import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.SecurityEventDetailedResponse;
import com.example.secrets_manager.api.rest.dto.SecurityEventSummaryResponse;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;

/** E2E REST client for the Admin Security Event Log API. */
public class SecurityEventClient {

  private final String token;

  public SecurityEventClient(String token) {
    this.token = token;
  }

  public PagedResponse<SecurityEventSummaryResponse> list(Map<String, ?> queryParams) {
    return listRaw(queryParams)
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<PagedResponse<SecurityEventSummaryResponse>>() {});
  }

  public SecurityEventDetailedResponse get(UUID id) {
    return getRaw(id).then().statusCode(200).extract().as(SecurityEventDetailedResponse.class);
  }

  public Response listRaw(Map<String, ?> queryParams) {
    return given()
        .header("Authorization", "Bearer " + token)
        .queryParams(queryParams)
        .when()
        .get("/api/v1/admin/security-events");
  }

  public Response getRaw(UUID id) {
    return given()
        .header("Authorization", "Bearer " + token)
        .pathParam("id", id)
        .when()
        .get("/api/v1/admin/security-events/{id}");
  }
}
