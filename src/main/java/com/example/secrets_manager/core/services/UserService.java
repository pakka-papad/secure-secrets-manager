package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.UserEntityConverter;
import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.models.UserCreationPayload;
import com.example.secrets_manager.core.services.exceptions.UserAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.UserServiceException;
import com.example.secrets_manager.crypto.CryptographyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final CryptographyService cryptographyService;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  @Autowired
  public UserService(
      UserRepository userRepository,
      CryptographyService cryptographyService,
      AuditService auditService,
      ObjectMapper objectMapper) {
    this.userRepository = userRepository;
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
  public User createUser(UserCreationPayload payload) {
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
}
