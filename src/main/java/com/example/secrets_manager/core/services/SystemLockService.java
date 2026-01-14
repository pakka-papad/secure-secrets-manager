package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.repositories.SystemLockRepository;
import com.example.secrets_manager.core.models.SystemLockName;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemLockService {

    private final SystemLockRepository systemLockRepository;

    @Autowired
    public SystemLockService(SystemLockRepository systemLockRepository) {
        this.systemLockRepository = systemLockRepository;
    }

    /**
     * Acquires an exclusive, pessimistic write lock for the given lock name.
     * This method will block for a maximum of 5 seconds (as configured in the repository)
     * until the lock is acquired. If the lock cannot be acquired within this timeout,
     * a {@code PessimisticLockingFailureException} will be thrown, causing the transaction to roll back.
     * The lock is held until the surrounding transaction commits or rolls back.
     *
     * @param lockName The name of the lock to acquire.
     * @throws EntityNotFoundException if the specified lockName does not exist in the database.
     * @throws org.springframework.dao.PessimisticLockingFailureException if the lock cannot be acquired within the configured timeout.
     */
    @Transactional // Default Propagation.REQUIRED: joins existing transaction or starts a new one.
    public void acquireExclusiveLock(SystemLockName lockName) {
        systemLockRepository.findAndLockByName(lockName.name())
                .orElseThrow(() -> new EntityNotFoundException("System lock with name '" + lockName.name() + "' not found."));
    }
}
