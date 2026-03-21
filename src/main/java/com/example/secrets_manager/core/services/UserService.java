package com.example.secrets_manager.core.services;

import com.example.secrets_manager.api.rest.dto.UserSearchCriteria;
import com.example.secrets_manager.core.data.converters.UserEntityConverter;
import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.data.repositories.UserSpecifications;
import com.example.secrets_manager.core.models.*;
import com.example.secrets_manager.core.models.events.UserDeletedEvent;
import com.example.secrets_manager.core.models.events.UserPasswordUpdatedEvent;
import com.example.secrets_manager.core.models.events.UserRolesUpdatedEvent;
import com.example.secrets_manager.core.services.exceptions.AdminDemotionException;
import com.example.secrets_manager.core.services.exceptions.InvalidPasswordException;
import com.example.secrets_manager.core.services.exceptions.SelfDeletionException;
import com.example.secrets_manager.core.services.exceptions.SelfDemotionException;
import com.example.secrets_manager.core.services.exceptions.UserAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.UserServiceException;
import com.example.secrets_manager.core.utils.CoreUtils;
import com.example.secrets_manager.core.utils.PaginationUtils;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UserService {

  private final UserRepository userRepository;
  private final CryptographyService cryptographyService;
  private final AuditService auditService;
  private final SystemLockService systemLockService;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;

  @Autowired
  public UserService(
      UserRepository userRepository,
      CryptographyService cryptographyService,
      AuditService auditService,
      SystemLockService systemLockService,
      ApplicationEventPublisher eventPublisher,
      ObjectMapper objectMapper) {
    this.userRepository = userRepository;
    this.cryptographyService = cryptographyService;
    this.auditService = auditService;
    this.systemLockService = systemLockService;
    this.eventPublisher = eventPublisher;
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
      savedUserEntity = userRepository.saveAndFlush(userEntity);
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("uq_sm_users_active_name")) {
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

    // 5. Global Logout (Side effect via Event)
    eventPublisher.publishEvent(new UserPasswordUpdatedEvent(userEntity.getId()));

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

    EnumSet<UserRole> oldRoles = EnumSet.noneOf(UserRole.class);
    for (String role : userEntity.getRoles()) {
      oldRoles.add(UserRole.valueOf(role));
    }

    // 5. Invariant Safety Check: Prevent removing the last admin
    boolean isTargetCurrentlyAdmin = oldRoles.contains(UserRole.ADMIN);
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

    // 7. Publish event for side-effects
    eventPublisher.publishEvent(new UserRolesUpdatedEvent(userId, oldRoles, newRoles));

    // 8. Create audit log entry
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(authenticatedUserId)
            .action(AuditAction.USER_ROLES_UPDATE)
            .targetUserId(userId)
            .build());

    return UserEntityConverter.toModel(savedUser);
  }

  /**
   * Soft-deletes a user from the system. This operation is restricted to administrators and
   * includes safety checks to prevent self-deletion and the removal of the last administrator.
   *
   * @param userId The UUID of the user to delete.
   * @throws EntityNotFoundException if the user is not found.
   * @throws SelfDeletionException if an administrator attempts to delete their own account.
   * @throws AdminDemotionException if the deletion would leave the system without an administrator.
   * @throws UserServiceException for other internal errors.
   */
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteUser(UUID userId)
      throws UserServiceException,
          EntityNotFoundException,
          SelfDeletionException,
          AdminDemotionException {
    // 1. Identify the Actor (Admin) from Security Context
    final var authenticatedUserId = SecurityUtils.getAuthenticatedUserId();

    // 2. Prevent self-deletion
    if (authenticatedUserId.equals(userId)) {
      throw new SelfDeletionException("Administrators cannot delete their own account.");
    }

    // 3. Serialize User Management globally to prevent demotion race conditions
    systemLockService.acquireExclusiveLock(SystemLockName.USER_ROLE_MANAGEMENT);

    // 4. Find and Lock the user row
    var userEntity =
        userRepository
            .findAndLockById(userId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format("User not found with ID: %s", userId)));

    // 5. Safety Check: Prevent removing the last admin
    boolean isTargetAdmin = Arrays.asList(userEntity.getRoles()).contains(UserRole.ADMIN.name());
    if (isTargetAdmin) {
      long adminCount = userRepository.countActiveAdmins();
      if (adminCount <= 1) {
        throw new AdminDemotionException(
            "Cannot delete the last administrator. System must have at least one active administrator.");
      }
    }

    // 6. Perform Soft Delete and scrub sensitive data
    userEntity.setDeletedAt(Instant.now());
    userEntity.setPwSalt(new byte[0]);
    userEntity.setPwDigest(new byte[0]);
    userEntity.setHashAlgo("SCRUBBED");
    userEntity.setHashParams("{}");
    userRepository.save(userEntity);

    // 7. Publish Event for side-effects (token deletion, authorization cleanup)
    eventPublisher.publishEvent(new UserDeletedEvent(userId));

    // 8. Create audit log entry
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(authenticatedUserId)
            .action(AuditAction.USER_DELETE)
            .targetUserId(userId)
            .build());
  }

  /**
   * Retrieves a paginated list of users based on the provided search criteria.
   *
   * @param criteria The {@link UserSearchCriteria} containing filters.
   * @param pageable The {@link Pageable} object for pagination and sorting.
   * @return A {@link Page} of {@link User} models.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<User> listUsers(UserSearchCriteria criteria, Pageable pageable) {
    PaginationUtils.validateSort(pageable, UserEntity.ALLOWED_SORT_FIELDS);
    Specification<UserEntity> spec = UserSpecifications.withCriteria(criteria);
    return userRepository.findAll(spec, pageable).map(UserEntityConverter::toModel);
  }

  /**
   * Retrieves the profile of the currently authenticated user.
   *
   * @return The {@link User} domain model of the current user.
   * @throws EntityNotFoundException if the user record is not found.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public User getCurrentUser() {
    final var authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
    return userRepository
        .findByIdAndDeletedAtIsNull(authenticatedUserId)
        .map(UserEntityConverter::toModel)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    String.format("User not found with ID: %s", authenticatedUserId)));
  }
}
