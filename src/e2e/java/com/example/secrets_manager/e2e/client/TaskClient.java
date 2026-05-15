package com.example.secrets_manager.e2e.client;

import static io.restassured.RestAssured.given;

import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.TaskDetailedResponse;
import com.example.secrets_manager.api.rest.dto.TaskSummaryResponse;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;

/** E2E REST client for the Admin Task API. */
public class TaskClient {

  private final String token;

  public TaskClient(String token) {
    this.token = token;
  }

  public PagedResponse<TaskSummaryResponse> listTasks(Map<String, ?> queryParams) {
    return listTasksRaw(queryParams).then().statusCode(200).extract().as(new TypeRef<>() {});
  }

  public TaskDetailedResponse getTask(UUID taskId) {
    return getTaskRaw(taskId).then().statusCode(200).extract().as(TaskDetailedResponse.class);
  }

  public Response getTaskRaw(UUID taskId) {
    return given()
        .header("Authorization", "Bearer " + token)
        .pathParam("taskId", taskId)
        .when()
        .get("/api/v1/admin/tasks/{taskId}");
  }

  public Response listTasksRaw(Map<String, ?> queryParams) {
    return given()
        .header("Authorization", "Bearer " + token)
        .queryParams(queryParams)
        .when()
        .get("/api/v1/admin/tasks");
  }
}
