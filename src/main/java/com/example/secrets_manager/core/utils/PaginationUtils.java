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
   * Validates that the sort properties in the given Pageable are within the allowed whitelist.
   *
   * @param pageable The Pageable object containing sort information.
   * @param allowedFields A set of field names that are permitted for sorting.
   * @throws IllegalArgumentException if an invalid sort field is detected.
   */
  public static void validateSort(Pageable pageable, Set<String> allowedFields) {
    if (pageable == null || pageable.getSort().isUnsorted()) {
      return;
    }

    for (Sort.Order order : pageable.getSort()) {
      if (!allowedFields.contains(order.getProperty())) {
        throw new IllegalArgumentException(
            String.format(
                "Sorting by field '%s' is unknown. Allowed fields: %s",
                order.getProperty(), allowedFields));
      }
    }
  }
}
