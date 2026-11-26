package com.example.eduhub.auth;

import com.example.eduhub.auth.domain.Role;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/**
 * 토큰에서 꺼낸 현재 사용자 (NFR-03). from이 던지는 InvalidBearerTokenException은 ExceptionTranslationFilter가 401로
 * 바꾼다. GlobalExceptionHandler에 @ExceptionHandler(Exception.class)를 추가하면 500이 되니 주의한다.
 */
public final class CurrentUser {

  private static final String INVALID_MESSAGE = "토큰의 사용자 정보가 올바르지 않습니다.";

  private final Long employeeId;
  private final Role role;

  private CurrentUser(Long employeeId, Role role) {
    this.employeeId = employeeId;
    this.role = role;
  }

  /** sub·role이 없거나 sub가 숫자가 아니거나 role이 잘못되면 InvalidBearerTokenException(401)을 던진다. */
  public static CurrentUser from(Jwt jwt) {
    String subject = jwt.getSubject();
    String role = jwt.getClaimAsString("role");
    if (subject == null || role == null) {
      throw new InvalidBearerTokenException(INVALID_MESSAGE);
    }
    try {
      return new CurrentUser(Long.valueOf(subject), Role.valueOf(role));
    } catch (IllegalArgumentException e) {
      throw new InvalidBearerTokenException(INVALID_MESSAGE);
    }
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public Role getRole() {
    return role;
  }
}
