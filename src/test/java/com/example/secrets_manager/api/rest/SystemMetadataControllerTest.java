package com.example.secrets_manager.api.rest;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.config.JacksonConfig;
import com.example.secrets_manager.config.TestSecurityConfig;
import com.example.secrets_manager.core.services.JwtTokenService;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.SymmetricAlgorithmMetadata;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(controllers = SystemMetadataController.class)
@Import({JacksonConfig.class, TestSecurityConfig.class})
class SystemMetadataControllerTest {

  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;

  @MockitoBean private CryptographyService cryptographyService;
  @MockitoBean private JwtTokenService jwtTokenService;
  @MockitoBean private CacheManager cacheManager;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser
  void getSymmetricAlgorithms_ShouldReturnMetadataList() throws Exception {
    // Given
    var metadata = new SymmetricAlgorithmMetadata("AES-256-GCM", 32);
    when(cryptographyService.getSupportedSymmetricAlgorithms()).thenReturn(List.of(metadata));

    // When
    mockMvc
        .perform(get("/api/v1/system/algorithms/symmetric"))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("AES-256-GCM"))
        .andExpect(jsonPath("$[0].keySizeBytes").value(32));
  }

  @Test
  void getSymmetricAlgorithms_WhenUnauthenticated_ShouldReturn401() throws Exception {
    // When
    mockMvc
        .perform(get("/api/v1/system/algorithms/symmetric"))
        // Then
        .andExpect(status().isUnauthorized());
  }
}
