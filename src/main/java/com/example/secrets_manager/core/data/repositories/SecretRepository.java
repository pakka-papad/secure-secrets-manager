package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecretRepository extends JpaRepository<SecretEntity, UUID> {
    Optional<SecretEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<SecretEntity> findAllByDeletedAtIsNull();
}
