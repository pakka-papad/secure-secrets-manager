package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.UserEntityConverter;
import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.*;
import com.example.secrets_manager.core.services.exceptions.AdminDemotionException;
import com.example.secrets_manager.core.services.exceptions.InvalidPasswordException;
import com.example.secrets_manager.core.services.exceptions.SelfDemotionException;
import com.example.secrets_manager.core.services.exceptions.UserAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.UserServiceException;
import com.example.secrets_manager.core.utils.CoreUtils;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final SystemLockService systemLockService;
  private final ObjectMapper objectMapper;

  @Autowired
  public UserService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      CryptographyService cryptographyService,
      AuditService auditService,
      SystemLockService systemLockService,
      ObjectMapper objectMapper) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.cryptographyService = cryptographyService;
    this.auditService = auditService;
    this.systemLockService = systemLockService;
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
  @PreAuthorize("hasRole('ADMIN')")
  public User createUser(@NotNull @Valid UserCreationPayload payload) throws UserServiceException {
    // 1. Identify the Actor (Admin) from Security Context
    final var authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

    // 2. Role Assignment: Ensure new user always has the USER role
    final var assignedRoles =
        (payload.getRoles() == null) ? EnumSet.noneOf(UserRole.class) : payload.getRoles();
    assignedRoles.add(UserRole.USER);

    // 3. Hash the password
    var hashedPassword = cryptographyService.hashPassword(payload.getPassword());

    // 4. Convert hash params Map to JSON string
    var hashParamsJson = "";
    try {
      hashParamsJson = objectMapper.writeValueAsString(hashedPassword.getParams());
    } catch (JsonProcessingException e) {
      throw new UserServiceException("Failed to serialize password hash parameters.", e);
    }

    // 5. Build the UserEntity
    var rolesArray = assignedRoles.stream().map(Enum::name).toArray(String[]::new);

    var userEntity =
        UserEntity.builder()
            .name(payload.getName())
            .pwSalt(hashedPassword.getSalt())
            .pwDigest(hashedPassword.getDigest())
            .hashAlgo(hashedPassword.getAlgorithm())
            .hashParams(hashParamsJson)
            .roles(rolesArray)
            .deletedAt(null)
            .build();

    // 6. Save the user entity
    UserEntity savedUserEntity;
    try {
      savedUserEntity = userRepository.save(userEntity);
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("uq_sm_users_name")) {
        throw new UserAlreadyExistsException(
            String.format("User with name '%s' already exists.", payload.getName()), e);
      }
      throw new UserServiceException("Failed to create user due to data integrity violation.", e);
    } catch (Exception e) {
      throw new UserServiceException("Failed to create user due to an unexpected error.", e);
    }

    // 7. Create audit log entry
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(authenticatedUserId)
            .action(AuditAction.USER_CREATE)
            .targetUserId(savedUserEntity.getId())
            .build());

    return UserEntityConverter.toModel(savedUserEntity);
  }

  /**
   * Updates the password for a specified user. This action also invalidates all active refresh
   * tokens for the user, effectively performing a global logout.
   *
   * @param payload The {@link UserPasswordUpdatePayload} containing user ID, old and new password.
   * @return The updated {@link User} domain model.
   * @throws EntityNotFoundException if the user is not found.
   * @throws InvalidPasswordException if the provided old password does not match the current one.
   * @throws UserServiceException for internal errors like JSON serialization.
   */
  @Transactional
  @PreAuthorize("isAuthenticated()")
  public User updatePassword(@NotNull @Valid UserPasswordUpdatePayload payload)
      throws UserServiceException, EntityNotFoundException, InvalidPasswordException {
    // 1. Identify the authenticated user
    final var authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

    // 2. Find the active user
    var userEntity =
        userRepository
            .findByIdAndDeletedAtIsNull(authenticatedUserId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format("User not found with ID: %s", authenticatedUserId)));

    // 3. Verify the old password
    var storedHash =
        new HashedPassword(
            userEntity.getPwDigest(),
            userEntity.getPwSalt(),
            userEntity.getHashAlgo(),
            CoreUtils.jsonStringToObjectMap(objectMapper, userEntity.getHashParams()));

    if (!cryptographyService.verifyPassword(payload.getOldPassword(), storedHash)) {
      throw new InvalidPasswordException("The provided old password does not match.");
    }

    // 4. Hash the new password and update
    var newHashedPassword = cryptographyService.hashPassword(payload.getNewPassword());
    userEntity.setPwSalt(newHashedPassword.getSalt());
    userEntity.setPwDigest(newHashedPassword.getDigest());
    userEntity.setHashAlgo(newHashedPassword.getAlgorithm());
    try {
      userEntity.setHashParams(objectMapper.writeValueAsString(newHashedPassword.getParams()));
    } catch (JsonProcessingException e) {
      throw new UserServiceException("Failed to serialize new password hash parameters.", e);
    }
    var savedUser = userRepository.save(userEntity);

    // 5. Global Logout
    refreshTokenRepository.deleteByUserId(userEntity.getId());

    // 6. Create audit log entry
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(userEntity.getId())
            .action(AuditAction.USER_PASSWORD_UPDATE)
            .targetUserId(userEntity.getId())
            .build());

    return UserEntityConverter.toModel(savedUser);
  }

  /**
   * Updates the roles for a specified user. This operation is restricted to administrators.
   * Includes safety checks to prevent self-modification and the demotion of the last human
   * administrator.
   *
   * @param userId The UUID of the user to update.
   * @param roles The new set of roles to assign.
   * @return The updated {@link User} domain model.
   * @throws EntityNotFoundException if the user is not found.
   * @throws AdminDemotionException if the update would leave the system without an administrator.
   * @throws SelfDemotionException if an administrator attempts to modify their own roles.
   * @throws UserServiceException for other internal errors.
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public User updateRoles(UUID userId, EnumSet<UserRole> roles)
      throws UserServiceException,
          EntityNotFoundException,
          AdminDemotionException,
          SelfDemotionException {
    // 1. Identify the Actor (Admin) from Security Context
    final var authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

    // 2. Fast Path: Prevent self-modification immediately
    if (authenticatedUserId.equals(userId)) {
      throw new SelfDemotionException("Administrators cannot modify their own roles.");
    }

    // 3. Conditional Global Locking: Only serialize if ADMIN role is being potentially removed
    if (!roles.contains(UserRole.ADMIN)) {
      systemLockService.acquireExclusiveLock(SystemLockName.USER_ROLE_MANAGEMENT);
    }

    // 4. Find and Lock the user
    var userEntity =
        userRepository
            .findAndLockById(userId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format("User not found with ID: %s", userId)));

    // 5. Invariant Safety Check: Prevent removing the last admin
    boolean isTargetCurrentlyAdmin =
        Arrays.asList(userEntity.getRoles()).contains(UserRole.ADMIN.name());
    boolean willBeAdmin = roles.contains(UserRole.ADMIN);

    if (isTargetCurrentlyAdmin && !willBeAdmin) {
      long adminCount = userRepository.countActiveAdmins();
      if (adminCount <= 1) {
        throw new AdminDemotionException(
            "Cannot remove ADMIN role. System must have at least one active administrator.");
      }
    }

    // 6. Apply role update: Ensure USER role is always present
    final var newRoles = EnumSet.copyOf(roles);
    newRoles.add(UserRole.USER);
    userEntity.setRoles(newRoles.stream().map(Enum::name).toArray(String[]::new));
    var savedUser = userRepository.save(userEntity);

    // 7. Create audit log entry
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(authenticatedUserId)
            .action(AuditAction.USER_ROLES_UPDATE)
            .targetUserId(userId)
            .build());

    return UserEntityConverter.toModel(savedUser);
  }
}
