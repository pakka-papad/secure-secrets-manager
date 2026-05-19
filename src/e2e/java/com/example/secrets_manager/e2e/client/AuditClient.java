package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;

import com.example.secrets_manager.api.rest.dto.AuditLogDetailedResponse;
import com.example.secrets_manager.api.rest.dto.AuditLogSummaryResponse;
import com.example.secrets_manager.api.rest.dto.PagedResponse;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.util.Map;

/** E2E REST client for the Admin Audit Log API. */
public class AuditClient {

  private final String token;

  public AuditClient(String token) {
    this.token = token;
  }

  public PagedResponse<AuditLogSummaryResponse> list(Map<String, ?> queryParams) {
    return listRaw(queryParams).then().statusCode(200).extract().as(new TypeRef<>() {});
  }

  public AuditLogDetailedResponse get(Long seqId) {
    return getRaw(seqId).then().statusCode(200).extract().as(AuditLogDetailedResponse.class);
  }

  public Response listRaw(Map<String, ?> queryParams) {
    return given()
        .header("Authorization", "Bearer " + token)
        .queryParams(queryParams)
        .when()
        .get("/api/v1/admin/audit-logs");
  }

  public Response getRaw(Long seqId) {
    return given()
        .header("Authorization", "Bearer " + token)
        .pathParam("seqId", seqId)
        .when()
        .get("/api/v1/admin/audit-logs/{seqId}");
  }
}
