package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.converters.SecurityEventLogResponseConverter;
import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.SecurityEventDetailedResponse;
import com.example.secrets_manager.api.rest.dto.SecurityEventSummaryResponse;
import com.example.secrets_manager.core.models.search.SecurityEventSearchCriteria;
import com.example.secrets_manager.core.services.SecurityEventLogService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API Controller for administrative security event visibility. */
@RestController
@RequestMapping(
    value = "/api/v1/admin/security-events",
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin: Security Events", description = "Administrative security event monitoring")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminSecurityEventController {

  private final SecurityEventLogService securityEventLogService;

  @Operation(
      summary = "List all security events with filters",
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
      description = "Security events retrieved successfully",
      content = @Content(schema = @Schema(implementation = PagedResponse.class)))
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<PagedResponse<SecurityEventSummaryResponse>> listSecurityEvents(
      @ParameterObject SecurityEventSearchCriteria criteria, @ParameterObject Pageable pageable) {
    var page = securityEventLogService.listSecurityEvents(criteria, pageable);
    var response =
        PagedResponse.fromPage(page.map(SecurityEventLogResponseConverter::toSummaryResponse));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get full details of a specific security event")
  @ApiResponse(
      responseCode = "200",
      description = "Security event details retrieved successfully",
      content = @Content(schema = @Schema(implementation = SecurityEventDetailedResponse.class)))
  @ApiResponse(responseCode = "404", description = "Security event not found")
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{id}")
  public ResponseEntity<SecurityEventDetailedResponse> getSecurityEventById(@PathVariable UUID id) {
    var event = securityEventLogService.getSecurityEventById(id);
    return ResponseEntity.ok(SecurityEventLogResponseConverter.toDetailedResponse(event));
  }
}
