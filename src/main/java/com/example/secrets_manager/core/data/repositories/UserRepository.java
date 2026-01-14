package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<UserEntity> findAllByDeletedAtIsNull();
}
