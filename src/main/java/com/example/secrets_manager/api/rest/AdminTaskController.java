package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.TaskResponseConverter;
import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.TaskDetailedResponse;
import com.example.secrets_manager.api.rest.dto.TaskSummaryResponse;
import com.example.secrets_manager.tasks.models.TaskSearchCriteria;
import com.example.secrets_manager.tasks.services.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API Controller for administrative task management and monitoring. */
@RestController
@RequestMapping(value = "/api/v1/admin/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin: Tasks", description = "Administrative background task monitoring")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminTaskController {

  private final TaskService taskService;

  @Operation(
      summary = "List all background tasks with filters",
      parameters = {
        @Parameter(
            name = "page",
            in = ParameterIn.QUERY,
            description = "Zero-based page index (offset-based)",
            schema = @Schema(type = "integer", defaultValue = "0")),
        @Parameter(
            name = "size",
            in = ParameterIn.QUERY,
            description = "Number of records per page (limit)",
            schema = @Schema(type = "integer", defaultValue = "50")),
        @Parameter(
            name = "sort",
            in = ParameterIn.QUERY,
            description = "Ignored. Results are always sorted in reverse chronological order.",
            schema = @Schema(type = "string", hidden = true))
      })
  @ApiResponse(
      responseCode = "200",
      description = "List retrieved successfully",
      content = @Content(schema = @Schema(implementation = PagedResponse.class)))
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden: Admin role required")
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<PagedResponse<TaskSummaryResponse>> listTasks(
      @ParameterObject TaskSearchCriteria criteria, @ParameterObject Pageable pageable) {
    var page = taskService.listTasks(criteria, pageable);
    var response = PagedResponse.fromPage(page.map(TaskResponseConverter::toSummaryResponse));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get full details of a specific task")
  @ApiResponse(
      responseCode = "200",
      description = "Task details retrieved successfully",
      content = @Content(schema = @Schema(implementation = TaskDetailedResponse.class)))
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden: Admin role required")
  @ApiResponse(responseCode = "404", description = "Task not found")
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{taskId}")
  public ResponseEntity<TaskDetailedResponse> getTaskById(@PathVariable UUID taskId) {
    var task = taskService.getTaskById(taskId);
    return ResponseEntity.ok(TaskResponseConverter.toDetailedResponse(task));
  }

  @Operation(summary = "Cancel a background task")
  @ApiResponse(
      responseCode = "200",
      description = "Cancellation attempt completed. Returns latest task state.",
      content = @Content(schema = @Schema(implementation = TaskDetailedResponse.class)))
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "403", description = "Forbidden: Admin role required")
  @ApiResponse(responseCode = "404", description = "Task not found")
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/{taskId}/cancel")
  public ResponseEntity<TaskDetailedResponse> cancelTask(@PathVariable UUID taskId) {
    var task = taskService.cancelTask(taskId);
    return ResponseEntity.ok(TaskResponseConverter.toDetailedResponse(task));
  }
}
