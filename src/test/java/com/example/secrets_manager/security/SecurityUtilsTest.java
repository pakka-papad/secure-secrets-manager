package com.example.secrets_manager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.secrets_manager.core.models.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void prefixRole_ShouldAddPrefix() {
    assertThat(SecurityUtils.prefixRole("ADMIN")).isEqualTo("ROLE_ADMIN");
    assertThat(SecurityUtils.prefixRole("ROLE_ADMIN")).isEqualTo("ROLE_ADMIN");
  }

  @Test
  void prefixRoles_ShouldAddPrefixToCollection() {
    List<String> prefixed = SecurityUtils.prefixRoles(List.of(UserRole.ADMIN, UserRole.USER));
    assertThat(prefixed).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
  }

  @Test
  void toAuthorities_ShouldConvertRolesToGrantedAuthorities() {
    Collection<? extends GrantedAuthority> authorities =
        SecurityUtils.toAuthorities(List.of(UserRole.ADMIN));
    assertThat(authorities).hasSize(1);
    assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
  }

  @Test
  void getAuthenticatedUserId_ShouldReturnUuid() {
    UUID userId = UUID.randomUUID();
    var auth = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(SecurityUtils.getAuthenticatedUserId()).isEqualTo(userId);
  }

  @Test
  void getAuthenticatedUserId_WhenNotAuthenticated_ShouldThrowException() {
    assertThatThrownBy(SecurityUtils::getAuthenticatedUserId)
        .isInstanceOf(NullPointerException.class);
  }
}
