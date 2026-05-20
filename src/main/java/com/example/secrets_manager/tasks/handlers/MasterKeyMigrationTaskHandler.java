package com.example.secrets_manager.tasks.handlers;

import com.example.secrets_manager.core.data.repositories.SecretRepository;
import com.example.secrets_manager.core.services.InternalSecretService;
import com.example.secrets_manager.core.services.exceptions.MasterKeyCompromisedException;
import com.example.secrets_manager.tasks.models.TaskContext;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationExtraInfo;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationInput;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationOutput;
import com.example.secrets_manager.tasks.services.AbstractTaskHandler;
import com.example.secrets_manager.tasks.services.TaskAssignmentService;
import com.example.secrets_manager.tasks.services.TaskExecutionOrchestrator;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import com.example.secrets_manager.tasks.services.exceptions.TaskCancelledException;
import com.example.secrets_manager.tasks.services.exceptions.TaskFencedUpdateFailedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Task handler for background master key migrations. Re-wraps all active secrets with a new active
 * master key.
 */
@Component
@Slf4j
public class MasterKeyMigrationTaskHandler
    extends AbstractTaskHandler<
        MasterKeyMigrationInput, MasterKeyMigrationOutput, MasterKeyMigrationExtraInfo> {

  private static final int BATCH_SIZE = 50;
  private static final int MAX_ERROR_DETAILS = 10;

  private final SecretRepository secretRepository;
  private final InternalSecretService internalSecretService;

  public MasterKeyMigrationTaskHandler(
      TaskExecutionOrchestrator orchestrator,
      TaskAssignmentService assignmentService,
      ApplicationEventPublisher eventPublisher,
      SecretRepository secretRepository,
      InternalSecretService internalSecretService) {
    super(orchestrator, assignmentService, eventPublisher);
    this.secretRepository = secretRepository;
    this.internalSecretService = internalSecretService;
  }

  @Override
  public TaskType getSupportedType() {
    return TaskType.MASTER_KEY_MIGRATION;
  }

  @Override
  public MasterKeyMigrationOutput execute(
      TaskContext<MasterKeyMigrationInput, MasterKeyMigrationExtraInfo> context) throws Exception {
    final int targetMkVersion = context.input().getTargetMasterKeyVersion();
    log.info(
        "Starting master key migration to version v{} for task {}",
        targetMkVersion,
        context.taskId());

    long successCount = 0;
    long failureCount = 0;
    final Map<UUID, String> errorDetails = new HashMap<>();

    List<UUID> secretIds;
    UUID lastId = null;

    do {

      secretIds =
          secretRepository.findSecretIdsByMasterKeyVersionLessThan(
              targetMkVersion, lastId, PageRequest.of(0, BATCH_SIZE));

      for (UUID secretId : secretIds) {
        try {
          // Perform the upgrade with an internal hard transactional fence
          internalSecretService.upgradeMasterKey(secretId, targetMkVersion, context.taskId());
          successCount++;
        } catch (TaskAssignmentEvictedException e) {
          // If we were evicted during the update, we must stop immediately
          log.error("Hard eviction detected for task {} during secret update.", context.taskId());
          abort(AbortReason.EVICTED, context.taskId());
        } catch (TaskCancelledException e) {
          // If we were cancelled during the update, stop immediately using framework abort
          log.info(
              "Graceful cancellation detected for task {} during secret update.", context.taskId());
          abort(AbortReason.CANCELLED, context.taskId());
        } catch (TaskFencedUpdateFailedException e) {
          // If a paradox is detected, stop immediately using framework abort
          log.error(
              "Integrity failure detected for task {} during secret update.", context.taskId());
          abort(AbortReason.INTEGRITY_FAILURE, context.taskId());
        } catch (MasterKeyCompromisedException e) {
          // If we hit a compromised key, stop the entire migration
          log.error(
              "Stopping migration for task {}: Master Key v{} is compromised.",
              context.taskId(),
              e.getMasterKeyVersion());
          abort(AbortReason.FAILURE, context.taskId());
        } catch (Exception e) {
          log.error("Failed to migrate secret {}: {}", secretId, e.getMessage());
          failureCount++;
          if (errorDetails.size() < MAX_ERROR_DETAILS) {
            errorDetails.put(secretId, e.getMessage());
          }
        }
      }

      // Maintain Keyset Pagination pointer
      if (!CollectionUtils.isEmpty(secretIds)) {
        lastId = secretIds.get(secretIds.size() - 1);
      }

      // Report Progress via the provided context channel
      // This also acts as a periodic heartbeat and fence check
      context
          .progressUpdater()
          .accept(
              MasterKeyMigrationExtraInfo.builder()
                  .processedSecrets(successCount + failureCount)
                  .failureCount(failureCount)
                  .build());

    } while (!secretIds.isEmpty());

    log.info(
        "Master key migration completed for task {}. Success: {}, Failures: {}",
        context.taskId(),
        successCount,
        failureCount);

    return MasterKeyMigrationOutput.builder()
        .successfullyMigrated(successCount)
        .totalFailures(failureCount)
        .errorDetails(errorDetails)
        .build();
  }
}
