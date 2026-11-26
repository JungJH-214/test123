package com.example.eduhub.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

/** JWT 설정 (NFR-03). 키는 환경 변수 EDUHUB_JWT_SECRET로만 받는다. */
@ConfigurationProperties("eduhub.jwt")
@ConstructorBinding
@Validated
public class JwtProperties {

  private static final int MIN_SECRET_BYTES = 32;

  @NotBlank private final String secret;
  @NotBlank private final String issuer;
  @NotNull private final Duration expiresIn;

  public JwtProperties(String secret, String issuer, Duration expiresIn) {
    this.secret = secret;
    this.issuer = issuer;
    this.expiresIn = expiresIn;
  }

  public String getSecret() {
    return secret;
  }

  public String getIssuer() {
    return issuer;
  }

  public Duration getExpiresIn() {
    return expiresIn;
  }

  /** HS256 서명 키. 32바이트 미만이면 시작할 때 실패한다. */
  public SecretKey secretKey() {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException("EDUHUB_JWT_SECRET은 32바이트 이상이어야 합니다");
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }
}
