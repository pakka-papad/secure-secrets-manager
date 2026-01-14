package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.AuthorizationEntity;
import com.example.secrets_manager.core.data.entities.AuthorizationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizationRepository
    extends JpaRepository<AuthorizationEntity, AuthorizationId> {}
