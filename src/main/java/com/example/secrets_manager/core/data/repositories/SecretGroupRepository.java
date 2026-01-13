package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecretGroupRepository extends JpaRepository<SecretGroupEntity, UUID> {
    Optional<SecretGroupEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<SecretGroupEntity> findAllByDeletedAtIsNull();
}
