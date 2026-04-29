package com.example.secrets_manager.tasks.data.repositories;

import com.example.secrets_manager.tasks.data.entities.TaskAssignmentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignmentEntity, UUID> {}
