package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.TaskEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {}
