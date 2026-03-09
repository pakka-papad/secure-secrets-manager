package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterKeyRepository
    extends JpaRepository<MasterKeyEntity, Integer>, JpaSpecificationExecutor<MasterKeyEntity> {}
