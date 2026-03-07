package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.api.rest.dto.UserSearchCriteria;
import com.example.secrets_manager.core.data.entities.UserEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

/** Dynamic specifications for building JPA queries for User entities. */
public final class UserSpecifications {

  private UserSpecifications() {}

  /** Builds a dynamic specification based on the provided search criteria. */
  public static Specification<UserEntity> withCriteria(UserSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // 1. Always exclude deleted users (Soft Delete)
      predicates.add(cb.isNull(root.get("deletedAt")));

      // 2. Filter by name (prefix match)
      if (!StringUtils.isBlank(criteria.getName())) {
        predicates.add(cb.like(root.get("name"), criteria.getName() + "%"));
      }

      // 3. Filter by role
      if (criteria.getRole() != null) {
        // Use cb.function for Postgres-specific array check compatibility
        predicates.add(
            cb.isTrue(
                cb.function(
                    "array_contains",
                    Boolean.class,
                    root.get("roles"),
                    cb.literal(criteria.getRole().name()))));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
