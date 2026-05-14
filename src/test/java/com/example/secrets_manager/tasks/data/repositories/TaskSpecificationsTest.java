package com.example.secrets_manager.tasks.data.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.secrets_manager.tasks.data.entities.TaskEntity;
import com.example.secrets_manager.tasks.models.TaskSearchCriteria;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class TaskSpecificationsTest {

  @Test
  @SuppressWarnings("unchecked")
  void withCriteria_Empty_ShouldReturnNullPredicate() {
    TaskSearchCriteria criteria = TaskSearchCriteria.builder().build();
    Specification<TaskEntity> spec = TaskSpecifications.withCriteria(criteria);

    Root<TaskEntity> root = mock(Root.class);
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    CriteriaBuilder cb = mock(CriteriaBuilder.class);

    when(cb.and(any(Predicate[].class))).thenReturn(null);

    Predicate result = spec.toPredicate(root, query, cb);

    assertThat(result).isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void withCriteria_Full_ShouldAddAllPredicates() {
    TaskSearchCriteria criteria =
        TaskSearchCriteria.builder()
            .types(EnumSet.of(TaskType.MASTER_KEY_MIGRATION))
            .states(EnumSet.of(TaskState.RUNNING))
            .build();

    Specification<TaskEntity> spec = TaskSpecifications.withCriteria(criteria);

    Root<TaskEntity> root = mock(Root.class);
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    CriteriaBuilder cb = mock(CriteriaBuilder.class);

    Path<Object> typePath = mock(Path.class);
    Path<Object> statePath = mock(Path.class);
    when(root.get(TaskEntity.COL_TYPE)).thenReturn(typePath);
    when(root.get(TaskEntity.COL_STATE)).thenReturn(statePath);

    Predicate typePredicate = mock(Predicate.class);
    Predicate statePredicate = mock(Predicate.class);
    when(typePath.in(any(Collection.class))).thenReturn(typePredicate);
    when(statePath.in(any(Collection.class))).thenReturn(statePredicate);

    spec.toPredicate(root, query, cb);

    verify(root).get(TaskEntity.COL_TYPE);
    verify(root).get(TaskEntity.COL_STATE);
  }
}
