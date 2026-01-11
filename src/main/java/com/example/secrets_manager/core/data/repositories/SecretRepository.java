package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.Secret;
import com.example.secrets_manager.core.data.entities.SecretGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecretRepository extends JpaRepository<Secret, UUID> {
}
