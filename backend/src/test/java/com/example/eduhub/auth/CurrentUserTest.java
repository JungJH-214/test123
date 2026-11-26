package com.example.eduhub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.eduhub.auth.domain.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class CurrentUserTest {

  private Jwt jwt(String subject, String role) {
    Jwt.Builder builder =
        Jwt.withTokenValue("t")
            .header("alg", "HS256")
            .subject(subject)
            .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
            .expiresAt(Instant.parse("2026-01-01T01:00:00Z"));
    if (role != null) {
      builder.claim("role", role);
    }
    return builder.build();
  }

  @Test
  void from_returnsEmployeeIdAndRole() {
    CurrentUser user = CurrentUser.from(jwt("7", "ADMIN"));

    assertThat(user.getEmployeeId()).isEqualTo(7L);
    assertThat(user.getRole()).isEqualTo(Role.ADMIN);
  }

  @Test
  void from_nonNumericSubject_throws() {
    assertThatThrownBy(() -> CurrentUser.from(jwt("abc", "EMPLOYEE")))
        .isInstanceOf(InvalidBearerTokenException.class);
  }

  @Test
  void from_unknownRole_throws() {
    assertThatThrownBy(() -> CurrentUser.from(jwt("7", "SUPERUSER")))
        .isInstanceOf(InvalidBearerTokenException.class);
  }

  @Test
  void from_missingRole_throws() {
    assertThatThrownBy(() -> CurrentUser.from(jwt("7", null)))
        .isInstanceOf(InvalidBearerTokenException.class);
  }
}
