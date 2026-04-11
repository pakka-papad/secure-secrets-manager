package com.example.secrets_manager.api.rest;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.config.JacksonConfig;
import com.example.secrets_manager.config.TestSecurityConfig;
import com.example.secrets_manager.core.services.JwtTokenService;
import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.SymmetricAlgorithmMetadata;
import java.util.List;
import java.util.Set;
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
  void getSymmetricAlgorithms_ShouldReturnAllMetadataList() throws Exception {
    // Given
    var metadata = new SymmetricAlgorithmMetadata("AES-256-GCM", 32, Set.of(CipherPurpose.DATA));
    when(cryptographyService.getSupportedAlgorithms()).thenReturn(List.of(metadata));

    // When
    mockMvc
        .perform(get("/api/v1/system/algorithms/symmetric"))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("AES-256-GCM"))
        .andExpect(jsonPath("$[0].keySizeBytes").value(32))
        .andExpect(jsonPath("$[0].supportedPurposes").isArray());
  }

  @Test
  @WithMockUser
  void getSymmetricAlgorithms_WithPurpose_ShouldReturnFilteredList() throws Exception {
    // Given
    var metadata = new SymmetricAlgorithmMetadata("AES-KW-256", 32, Set.of(CipherPurpose.KEY_WRAP));
    when(cryptographyService.getSupportedAlgorithms(CipherPurpose.KEY_WRAP))
        .thenReturn(List.of(metadata));

    // When
    mockMvc
        .perform(get("/api/v1/system/algorithms/symmetric").param("purpose", "KEY_WRAP"))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("AES-KW-256"))
        .andExpect(jsonPath("$[0].supportedPurposes[0]").value("KEY_WRAP"));
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
