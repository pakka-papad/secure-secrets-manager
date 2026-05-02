package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.repositories.WorkerRegistryRepository;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Service for managing distributed worker identity and health. Implements transaction-aware
 * heartbeats to ensure in-memory state never drifts from the database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkerService {

  private final WorkerRegistryRepository workerRegistryRepository;
  private final LocalTaskRegistry localTaskRegistry;
  private final Clock clock;

  @Value("${tasks.heartbeat.throttle-ms:5000}")
  private long heartbeatThrottleMs;

  private final AtomicReference<Instant> lastDbHeartbeat = new AtomicReference<>(Instant.EPOCH);

  /**
   * Registers/Refreshes this instance in the worker registry. Throttled to prevent DB spikes. State
   * is only updated after a successful transaction commit.
   */
  @Transactional
  public void registerWorker() {
    if (isUpdateRequired()) {
      performDbUpdate();
      syncMemoryAfterCommit();
    }
  }

  /** Periodic heartbeat to signal that this instance is healthy. Runs in its own transaction. */
  @Scheduled(fixedDelayString = "${tasks.heartbeat.interval:10000}")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void sendHeartbeat() {
    if (localTaskRegistry.hasActiveTasks() && isUpdateRequired()) {
      performDbUpdate();
      syncMemoryAfterCommit();
    }
  }

  /** Graceful cleanup on shutdown. */
  @PreDestroy
  @Transactional
  public void cleanup() {
    log.info("Cleaning up worker registry for {}", TaskUtils.WORKER_ID);
    try {
      workerRegistryRepository.deleteById(TaskUtils.WORKER_ID);
    } catch (Exception e) {
      log.warn("Failed to clean up worker registry row: {}", e.getMessage());
    }
  }

  /** Thread-safe check if a DB update is required based on the throttle. */
  private boolean isUpdateRequired() {
    Instant now = Instant.now(clock);
    Instant last = lastDbHeartbeat.get();
    return now.toEpochMilli() - last.toEpochMilli() > heartbeatThrottleMs;
  }

  /** Directly performs the DB write. */
  private void performDbUpdate() {
    workerRegistryRepository.upsertHeartbeat(TaskUtils.WORKER_ID);
    log.debug("Heartbeat DB update initiated for worker {}.", TaskUtils.WORKER_ID);
  }

  /**
   * Registers a synchronization hook to update the in-memory timestamp ONLY after a successful
   * commit. If the transaction rolls back, lastDbHeartbeat remains old, forcing a retry in the next
   * call.
   */
  private void syncMemoryAfterCommit() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              Instant commitTime = Instant.now(clock);
              lastDbHeartbeat.set(commitTime);
              log.trace(
                  "Heartbeat state synchronized with memory for worker {} at {}.",
                  TaskUtils.WORKER_ID,
                  commitTime);
            }
          });
    } else {
      // No active transaction, update immediately
      lastDbHeartbeat.set(Instant.now(clock));
    }
  }
}
