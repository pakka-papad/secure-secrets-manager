package com.example.secrets_manager.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class CorrelationFilterTest {

  private CorrelationFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new CorrelationFilter();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);
    ReflectionTestUtils.setField(filter, "trustExternalId", false);
  }

  @Test
  void doFilter_ShouldGenerateNewV7Id_WhenNoHeaderAndNoTrust()
      throws ServletException, IOException {
    filter.doFilter(request, response, filterChain);

    String correlationIdStr = response.getHeader("X-Correlation-ID");
    assertThat(correlationIdStr).isNotNull();
    UUID correlationId = UUID.fromString(correlationIdStr);
    assertThat(correlationId.version()).isEqualTo(7);

    verify(filterChain).doFilter(request, response);

    // Context should be cleared after filter
    assertThat(CorrelationContext.get()).isEmpty();
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  void doFilter_ShouldAdoptHeader_WhenTrustIsEnabled() throws ServletException, IOException {
    ReflectionTestUtils.setField(filter, "trustExternalId", true);
    UUID externalId = UUID.randomUUID();
    request.addHeader("X-Correlation-ID", externalId.toString());

    filter.doFilter(request, response, filterChain);

    assertThat(response.getHeader("X-Correlation-ID")).isEqualTo(externalId.toString());
  }

  @Test
  void doFilter_ShouldIgnoreHeader_WhenTrustIsDisabled() throws ServletException, IOException {
    ReflectionTestUtils.setField(filter, "trustExternalId", false);
    UUID externalId = UUID.randomUUID();
    request.addHeader("X-Correlation-ID", externalId.toString());

    filter.doFilter(request, response, filterChain);

    String adoptedId = response.getHeader("X-Correlation-ID");
    assertThat(adoptedId).isNotEqualTo(externalId.toString());
  }

  @Test
  void doFilter_ShouldGenerateNewId_WhenHeaderIsInvalid() throws ServletException, IOException {
    ReflectionTestUtils.setField(filter, "trustExternalId", true);
    request.addHeader("X-Correlation-ID", "not-a-uuid");

    filter.doFilter(request, response, filterChain);

    String newId = response.getHeader("X-Correlation-ID");
    assertThat(newId).isNotNull();
    assertThat(UUID.fromString(newId)).isNotNull();
  }

  @Test
  void doFilter_ShouldNeverLeakBetweenRequests_OnSameThread() throws ServletException, IOException {
    // First Request
    filter.doFilter(request, response, filterChain);
    String id1 = response.getHeader("X-Correlation-ID");

    // Second Request (same filter instance, same thread)
    MockHttpServletRequest request2 = new MockHttpServletRequest();
    MockHttpServletResponse response2 = new MockHttpServletResponse();
    filter.doFilter(request2, response2, filterChain);
    String id2 = response2.getHeader("X-Correlation-ID");

    assertThat(id1).isNotEqualTo(id2);
    assertThat(CorrelationContext.get()).isEmpty();
  }
}
