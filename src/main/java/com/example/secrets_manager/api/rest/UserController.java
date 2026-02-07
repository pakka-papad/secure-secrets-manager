package com.example.secrets_manager.api.rest;

import com.example.secrets_manager.api.rest.dto.UserResponse;
import com.example.secrets_manager.core.models.UserCreationPayload;
import com.example.secrets_manager.core.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "User management APIs")
public class UserController {

  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(summary = "Create a new user")
  @ApiResponse(
      responseCode = "201",
      description = "User created successfully",
      content = @Content(schema = @Schema(implementation = UserResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid input or password policies violated")
  @ApiResponse(responseCode = "409", description = "User with given name already exists")
  @ApiResponse(responseCode = "500", description = "Internal server error")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserResponse> createUser(
      @Valid @RequestBody UserCreationPayload payload) {
    var user = userService.createUser(payload);
    var userResponse =
        UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .createdAt(user.getCreatedAt())
            .modifiedAt(user.getModifiedAt())
            .build();
    return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
  }

}
