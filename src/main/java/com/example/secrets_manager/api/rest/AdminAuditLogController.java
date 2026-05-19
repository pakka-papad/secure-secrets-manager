package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.AuditLogResponseConverter;
import com.example.secrets_manager.api.rest.dto.AuditLogDetailedResponse;
import com.example.secrets_manager.api.rest.dto.AuditLogSummaryResponse;
import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.core.models.search.AuditLogSearchCriteria;
import com.example.secrets_manager.core.services.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API Controller for administrative forensic audit log visibility. */
@RestController
@RequestMapping(value = "/api/v1/admin/audit-logs", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin: Audit Logs", description = "Administrative forensic audit trail monitoring")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminAuditLogController {

  private final AuditService auditService;

  @Operation(
      summary = "List all audit logs with filters",
      parameters = {
        @Parameter(
            name = "page",
            in = ParameterIn.QUERY,
            description = "Zero-based page index",
            schema = @Schema(type = "integer", defaultValue = "0")),
        @Parameter(
            name = "size",
            in = ParameterIn.QUERY,
            description = "Number of records per page",
            schema = @Schema(type = "integer", defaultValue = "50")),
        @Parameter(
            name = "sort",
            in = ParameterIn.QUERY,
            description = "Ignored. Results are always sorted in reverse chronological order.",
            schema = @Schema(type = "string", hidden = true))
      })
  @ApiResponse(
      responseCode = "200",
      description = "Audit logs retrieved successfully",
      content = @Content(schema = @Schema(implementation = PagedResponse.class)))
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<PagedResponse<AuditLogSummaryResponse>> listAuditLogs(
      @ParameterObject AuditLogSearchCriteria criteria, @ParameterObject Pageable pageable) {
    var page = auditService.listAuditLogs(criteria, pageable);
    var response = PagedResponse.fromPage(page.map(AuditLogResponseConverter::toSummaryResponse));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get full details of a specific audit log by sequence ID")
  @ApiResponse(
      responseCode = "200",
      description = "Audit log details retrieved successfully",
      content = @Content(schema = @Schema(implementation = AuditLogDetailedResponse.class)))
  @ApiResponse(responseCode = "404", description = "Audit log not found")
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{seqId}")
  public ResponseEntity<AuditLogDetailedResponse> getAuditLogById(@PathVariable Long seqId) {
    var log = auditService.getAuditLogById(seqId);
    return ResponseEntity.ok(AuditLogResponseConverter.toDetailedResponse(log));
  }
}
