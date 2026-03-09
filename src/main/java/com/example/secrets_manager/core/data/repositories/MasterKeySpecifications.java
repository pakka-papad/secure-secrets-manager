package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import com.example.secrets_manager.core.models.search.MasterKeySearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

/** Dynamic specifications for building JPA queries for Master Key entities. */
public final class MasterKeySpecifications {

  private MasterKeySpecifications() {}

  /** Builds a dynamic specification based on the provided search criteria. */
  public static Specification<MasterKeyEntity> withCriteria(MasterKeySearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (!CollectionUtils.isEmpty(criteria.getStatuses())) {
        // Map Enum set to their String representations for the IN clause
        List<String> statusNames = criteria.getStatuses().stream().map(Enum::name).toList();
        predicates.add(root.get("status").in(statusNames));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
