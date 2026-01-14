package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SystemLock;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemLockRepository extends JpaRepository<SystemLock, String> {

    /**
     * Finds a lock by its name and acquires a pessimistic write lock on the corresponding row,
     * with a timeout. If the lock cannot be acquired within the timeout period, a
     * PessimisticLockingFailureException will be thrown, causing the transaction to roll back.
     *
     * @param lockName The name of the lock to acquire.
     * @return An Optional containing the SystemLock if found.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") // 5000ms = 5 seconds
    })
    @Query("SELECT l FROM SystemLock l WHERE l.lockName = :lockName")
    Optional<SystemLock> findAndLockByName(String lockName);
}
