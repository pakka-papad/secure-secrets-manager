package com.example.secrets_manager.core.utils;

import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Utility for handling pagination and sorting validation. */
public final class PaginationUtils {

  private PaginationUtils() {
    // Prevent instantiation
  }

  /**
   * Validates that the sort properties in the given Pageable are within the allowed whitelist and
   * that only a single sort dimension is provided.
   *
   * @param pageable The Pageable object containing sort information.
   * @param allowedFields A set of field names that are permitted for sorting.
   * @throws IllegalArgumentException if an invalid sort field or multiple sort dimensions are
   *     detected.
   */
  public static void validateSort(Pageable pageable, Set<String> allowedFields) {
    if (pageable == null || pageable.getSort().isUnsorted()) {
      return;
    }

    Sort sort = pageable.getSort();

    // 1. Enforce single-dimension sorting
    long sortCount = sort.stream().count();
    if (sortCount > 1) {
      throw new IllegalArgumentException("Only one sort dimension is allowed.");
    }

    // 2. Validate against whitelist
    for (Sort.Order order : sort) {
      if (!allowedFields.contains(order.getProperty())) {
        throw new IllegalArgumentException(
            String.format(
                "Sorting by field '%s' is unknown. Allowed fields: %s",
                order.getProperty(), allowedFields));
      }
    }
  }
}
