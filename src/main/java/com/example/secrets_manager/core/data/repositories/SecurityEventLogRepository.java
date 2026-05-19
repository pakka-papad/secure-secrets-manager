package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecurityEventLogEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityEventLogRepository
    extends JpaRepository<SecurityEventLogEntity, UUID>,
        JpaSpecificationExecutor<SecurityEventLogEntity> {}
