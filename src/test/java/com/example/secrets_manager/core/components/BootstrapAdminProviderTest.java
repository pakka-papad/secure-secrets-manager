package com.example.secrets_manager.core.components;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminProviderTest {

  @Mock private UserService userService;
  @Mock private UserRepository userRepository;

  @InjectMocks private BootstrapAdminProvider bootstrapProvider;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(bootstrapProvider, "initialAdminUsername", "admin");
    ReflectionTestUtils.setField(bootstrapProvider, "initialAdminPassword", "password123");
  }

  @Test
  void onApplicationReady_ShouldCreateAdmin_WhenNoneExist() {
    // Given
    when(userRepository.existsByRoleAdmin()).thenReturn(false);

    // When
    bootstrapProvider.onApplicationReady();

    // Then
    verify(userService, times(1)).createUser(any());
  }

  @Test
  void onApplicationReady_ShouldDoNothing_WhenAdminExists() {
    // Given
    when(userRepository.existsByRoleAdmin()).thenReturn(true);

    // When
    bootstrapProvider.onApplicationReady();

    // Then
    verify(userService, never()).createUser(any());
  }

  @Test
  void onApplicationReady_ShouldDoNothing_WhenCredentialsMissing() {
    // Given
    ReflectionTestUtils.setField(bootstrapProvider, "initialAdminUsername", "");
    when(userRepository.existsByRoleAdmin()).thenReturn(false);

    // When
    bootstrapProvider.onApplicationReady();

    // Then
    verify(userService, never()).createUser(any());
  }
}
