package com.example.secrets_manager.tasks.data.repositories;

import com.example.secrets_manager.tasks.data.entities.TaskEntity;
import com.example.secrets_manager.tasks.models.TaskSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Specifications for dynamic filtering of task entities. */
public final class TaskSpecifications {

  private TaskSpecifications() {
    // Prevent instantiation
  }

  public static Specification<TaskEntity> withCriteria(TaskSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (criteria.getTypes() != null && !criteria.getTypes().isEmpty()) {
        predicates.add(
            root.get(TaskEntity.COL_TYPE)
                .in(criteria.getTypes().stream().map(Enum::name).toList()));
      }

      if (criteria.getStates() != null && !criteria.getStates().isEmpty()) {
        predicates.add(
            root.get(TaskEntity.COL_STATE)
                .in(criteria.getStates().stream().map(Enum::name).toList()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
