package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.SecretEntity;
import com.example.secrets_manager.core.models.search.SecretSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

/** Utility for building JPA Specifications for SecretEntity. */
public class SecretSpecifications {

  public static Specification<SecretEntity> withCriteria(
      UUID groupId, SecretSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // 1. Mandatory Group ID filter
      predicates.add(cb.equal(root.get("groupId"), groupId));

      // 2. Mandatory Active (non-deleted) filter
      predicates.add(cb.isNull(root.get("deletedAt")));

      // 3. Optional Name Prefix filter (Case-Insensitive)
      if (criteria != null && StringUtils.isNotBlank(criteria.getNamePrefix())) {
        final var pattern = criteria.getNamePrefix().toLowerCase() + "%";
        predicates.add(cb.like(root.get("secretName"), pattern));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
