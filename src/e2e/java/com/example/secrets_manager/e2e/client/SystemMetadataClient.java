package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;

import com.example.secrets_manager.crypto.dto.SymmetricAlgorithmMetadata;
import io.restassured.common.mapper.TypeRef;
import java.util.List;

public class SystemMetadataClient {
  private final String token;

  public SystemMetadataClient(String token) {
    this.token = token;
  }

  public List<SymmetricAlgorithmMetadata> getSymmetricAlgorithms() {
    return given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/v1/system/algorithms/symmetric")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<>() {});
  }
}
