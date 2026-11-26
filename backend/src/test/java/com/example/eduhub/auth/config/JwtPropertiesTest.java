package com.example.eduhub.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

  @Test
  void secretKey_32bytesOrMore_returnsHmacKey() {
    JwtProperties props =
        new JwtProperties("0123456789abcdef0123456789abcdef", "eduhub", Duration.ofMinutes(60));

    assertThat(props.secretKey().getAlgorithm()).isEqualTo("HmacSHA256");
  }

  @Test
  void secretKey_shorterThan32bytes_throws() {
    JwtProperties props = new JwtProperties("short-secret", "eduhub", Duration.ofMinutes(60));

    assertThatThrownBy(props::secretKey).isInstanceOf(IllegalStateException.class);
  }
}
