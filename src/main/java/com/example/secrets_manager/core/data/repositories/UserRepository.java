package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<UserEntity> findAllByDeletedAtIsNull();
}

