package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.UserEntityConverter;
import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.models.UserCreationPayload;
import com.example.secrets_manager.core.models.UserPasswordUpdatePayload;
import com.example.secrets_manager.core.services.exceptions.InvalidPasswordException;
import com.example.secrets_manager.core.services.exceptions.UserAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.UserServiceException;
import com.example.secrets_manager.core.utils.CoreUtils;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UserService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final CryptographyService cryptographyService;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  @Autowired
  public UserService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      CryptographyService cryptographyService,
      AuditService auditService,
      ObjectMapper objectMapper) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.cryptographyService = cryptographyService;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a new user with the provided details. The password will be hashed and an audit log
   * entry will be created.
   *
   * @param payload The {@link UserCreationPayload} containing user details.
   * @return The created {@link User} domain model.
   * @throws UserAlreadyExistsException if a user with the given name already exists.
   * @throws UserServiceException if password hash parameters cannot be serialized or other
   *     unexpected service errors occur.
   */
  @Transactional
  public User createUser(@NotNull @Valid UserCreationPayload payload) throws UserServiceException {
    // 1. Hash the password
    var hashedPassword = cryptographyService.hashPassword(payload.getPassword());

    // 2. Convert hash params Map to JSON string
    var hashParamsJson = "";
    try {
      hashParamsJson = objectMapper.writeValueAsString(hashedPassword.getParams());
    } catch (JsonProcessingException e) {
      throw new UserServiceException("Failed to serialize password hash parameters.", e);
    }

    // 3. Build the UserEntity
    var userEntity =
        UserEntity.builder()
            .name(payload.getName())
            .pwSalt(hashedPassword.getSalt())
            .pwDigest(hashedPassword.getDigest())
            .hashAlgo(hashedPassword.getAlgorithm())
            .hashParams(hashParamsJson)
            // createdAt and modifiedAt are set by @PrePersist
            .deletedAt(null)
            .build();

    // 4. Save the user entity, relying on DB unique constraint for duplicate name check
    UserEntity savedUserEntity;
    try {
      savedUserEntity = userRepository.save(userEntity);
    } catch (DataIntegrityViolationException e) {
      // Check if the cause is a unique constraint violation on the name column
      if (e.getMessage() != null
          && e.getMessage()
              .contains("uq_sm_users_name")) { // Example for PostgreSQL unique constraint name
        throw new UserAlreadyExistsException(
            String.format("User with name '%s' already exists.", payload.getName()), e);
      }
      throw new UserServiceException("Failed to create user due to data integrity violation.", e);
    } catch (Exception e) {
      throw new UserServiceException(
          "Failed to create user due to an unexpected error during persistence.", e);
    }

    // 5. Create audit log entry
    var auditPayload =
        AuditLogPayload.builder()
            .actorUserId(savedUserEntity.getId())
            .action(AuditAction.USER_CREATE)
            .targetUserId(savedUserEntity.getId())
            .build();
    auditService.save(auditPayload);

    // 6. Convert and return the User domain model
    return UserEntityConverter.toModel(savedUserEntity);
  }

  /**
   * Updates the password for a specified user. This action also invalidates all active refresh
   * tokens for the user, effectively performing a global logout.
   *
   * @param payload The {@link UserPasswordUpdatePayload} containing user ID, old and new password.
   * @throws EntityNotFoundException if the user is not found.
   * @throws InvalidPasswordException if the provided old password does not match the current one.
   * @throws AccessDeniedException if the authenticated user is not authorized to change this
   *     password.
   * @throws UserServiceException for internal errors like JSON serialization.
   */
  @Transactional
  public void updatePassword(@NotNull @Valid UserPasswordUpdatePayload payload)
      throws UserServiceException,
          EntityNotFoundException,
          InvalidPasswordException,
          AccessDeniedException {
    // 1. Ownership Check: Ensure the authenticated user is changing their own password
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new AccessDeniedException("User is not authenticated");
    }

    var authenticatedUserIdStr = (String) auth.getPrincipal();
    final var authenticatedUserId =
        authenticatedUserIdStr != null ? UUID.fromString(authenticatedUserIdStr) : null;

    if (!Objects.equals(payload.getUserId(), authenticatedUserId)) {
      // Log the unauthorized attempt
      // We use null for targetUserId to avoid FK violations with untrusted payload data.
      // The attempted ID is stored safely in the details JSON.
      auditService.save(
          AuditLogPayload.builder()
              .actorUserId(authenticatedUserId)
              .action(AuditAction.ACCESS_DENIED)
              .targetUserId(null)
              .details(
                  String.format(
                      "{\"action\":\"password_update_attempt_on_other_user\", \"attempted_target_id\":\"%s\"}",
                      payload.getUserId()))
              .build());
      throw new AccessDeniedException(
          "You are not authorized to change the password for this user");
    }

    // 2. Find the active user
    var userEntity =
        userRepository
            .findByIdAndDeletedAtIsNull(payload.getUserId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format("User not found with ID: %s", payload.getUserId())));

    // 3. Verify the old password
    var storedHash =
        new HashedPassword(
            userEntity.getPwDigest(),
            userEntity.getPwSalt(),
            userEntity.getHashAlgo(),
            CoreUtils.jsonStringToObjectMap(objectMapper, userEntity.getHashParams()));
    boolean passwordMatches =
        cryptographyService.verifyPassword(payload.getOldPassword(), storedHash);
    if (!passwordMatches) {
      throw new InvalidPasswordException("The provided old password does not match.");
    }

    // 4. Hash the new password
    var newHashedPassword = cryptographyService.hashPassword(payload.getNewPassword());

    // 5. Update the user entity with the new password details
    userEntity.setPwSalt(newHashedPassword.getSalt());
    userEntity.setPwDigest(newHashedPassword.getDigest());
    userEntity.setHashAlgo(newHashedPassword.getAlgorithm());
    try {
      userEntity.setHashParams(objectMapper.writeValueAsString(newHashedPassword.getParams()));
    } catch (JsonProcessingException e) {
      throw new UserServiceException("Failed to serialize new password hash parameters.", e);
    }
    // The @PreUpdate annotation will handle the modifiedAt timestamp automatically
    userRepository.save(userEntity);

    // 6. Global Logout: Invalidate all existing refresh tokens for this user
    refreshTokenRepository.deleteByUserId(userEntity.getId());

    // 7. Create audit log entry
    var auditPayload =
        AuditLogPayload.builder()
            .actorUserId(userEntity.getId())
            .action(AuditAction.USER_PASSWORD_UPDATE)
            .targetUserId(userEntity.getId())
            .build();
    auditService.save(auditPayload);
  }
}
