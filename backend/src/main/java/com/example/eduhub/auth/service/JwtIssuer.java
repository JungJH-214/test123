package com.example.eduhub.auth.service;

import com.example.eduhub.auth.config.JwtProperties;
import com.example.eduhub.auth.domain.Employee;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** 사원에게 HS256 JWT를 발급한다. */
@Component
public class JwtIssuer {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;
  private final Clock clock;

  public JwtIssuer(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  public IssuedToken issue(Employee employee) {
    Instant now = clock.instant();
    Duration expiresIn = properties.getExpiresIn();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .subject(String.valueOf(employee.getId()))
            .claim("role", employee.getRole().name())
            .issuedAt(now)
            .expiresAt(now.plus(expiresIn))
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new IssuedToken(token, expiresIn.getSeconds());
  }

  /** 발급 결과: 토큰 문자열과 만료까지 초. */
  public static final class IssuedToken {

    private final String token;
    private final long expiresInSeconds;

    public IssuedToken(String token, long expiresInSeconds) {
      this.token = token;
      this.expiresInSeconds = expiresInSeconds;
    }

    public String getToken() {
      return token;
    }

    public long getExpiresInSeconds() {
      return expiresInSeconds;
    }
  }
}
