package com.example.secrets_manager.api.rest.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Generic wrapper for paginated API responses.
 *
 * @param <T> The type of items in the list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
  private List<T> items;
  private long totalElements;
  private int totalPages;
  private int pageNumber;
  private int pageSize;

  /** Helper to create a PagedResponse from a Spring Data Page. */
  public static <T> PagedResponse<T> fromPage(Page<T> page) {
    return PagedResponse.<T>builder()
        .items(page.getContent())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .pageNumber(page.getNumber())
        .pageSize(page.getSize())
        .build();
  }
}
