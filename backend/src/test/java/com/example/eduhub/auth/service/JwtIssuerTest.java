package com.example.eduhub.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.auth.config.JwtProperties;
import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtIssuerTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private JwtProperties properties;
  private JwtIssuer issuer;
  private NimbusJwtDecoder decoder;

  @BeforeEach
  void setUp() {
    properties =
        new JwtProperties(
            "test-only-jwt-secret-0123456789abcdef", "eduhub", Duration.ofMinutes(60));
    NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(properties.secretKey()));
    issuer = new JwtIssuer(encoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    decoder =
        NimbusJwtDecoder.withSecretKey(properties.secretKey())
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    // 고정 시각이 과거라 만료 검증에 걸리므로, 이 테스트는 클레임 내용만 본다.
    decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
  }

  private Employee employee() {
    Employee employee = Employee.create("E0001", "사원1", "hash", Role.EMPLOYEE);
    ReflectionTestUtils.setField(employee, "id", 7L);
    return employee;
  }

  @Test
  void issue_tokenHasSubjectRoleIssuerAndExpiry() {
    JwtIssuer.IssuedToken issued = issuer.issue(employee());

    Jwt jwt = decoder.decode(issued.getToken());
    assertThat(jwt.getSubject()).isEqualTo("7");
    assertThat(jwt.getClaimAsString("role")).isEqualTo("EMPLOYEE");
    assertThat(jwt.getClaimAsString("iss")).isEqualTo("eduhub");
    assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
    assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
    assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
  }

  @Test
  void issue_expiresInIs3600() {
    assertThat(issuer.issue(employee()).getExpiresInSeconds()).isEqualTo(3600L);
  }
}
