package com.example.secrets_manager.core.data.repositories;

import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import com.example.secrets_manager.core.models.search.AuditLogSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Specifications for dynamic filtering of audit log entities. */
public final class AuditLogSpecifications {

  private AuditLogSpecifications() {
    // Prevent instantiation
  }

  public static Specification<AuditLogEntity> withCriteria(AuditLogSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (criteria.getActorUserId() != null) {
        predicates.add(
            cb.equal(root.get(AuditLogEntity.COL_ACTOR_USER_ID), criteria.getActorUserId()));
      }

      if (criteria.getAction() != null) {
        predicates.add(cb.equal(root.get(AuditLogEntity.COL_ACTION), criteria.getAction().name()));
      }

      if (criteria.getTargetSecretId() != null) {
        predicates.add(
            cb.equal(root.get(AuditLogEntity.COL_TARGET_SECRET_ID), criteria.getTargetSecretId()));
      }

      if (criteria.getCorrelationId() != null) {
        predicates.add(
            cb.equal(root.get(AuditLogEntity.COL_CORRELATION_ID), criteria.getCorrelationId()));
      }

      if (criteria.getStartTime() != null) {
        predicates.add(
            cb.greaterThanOrEqualTo(
                root.get(AuditLogEntity.COL_CREATED_AT), criteria.getStartTime()));
      }

      if (criteria.getEndTime() != null) {
        predicates.add(
            cb.lessThanOrEqualTo(root.get(AuditLogEntity.COL_CREATED_AT), criteria.getEndTime()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
