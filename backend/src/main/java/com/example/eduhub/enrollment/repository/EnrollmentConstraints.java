package com.example.eduhub.enrollment.repository;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * enrollment 테이블의 제약 판정 도구.
 *
 * <p>Hibernate 5.6은 제약 이름을 DB 오류 메시지 문자열에서 잘라 낸다. PostgreSQL의 {@code lc_messages}가 한국어 등이면 메시지 형식이
 * 달라져 이름이 null이 되므로, 메시지 파싱만 믿지 않고 로캘과 무관한 SQLState도 함께 본다.
 */
public final class EnrollmentConstraints {

  public static final String ACTIVE_UNIQUE_INDEX = "ux_enrollment_session_employee_active";

  static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

  private static final int MAX_DEPTH = 10;

  private EnrollmentConstraints() {}

  /**
   * 저장 실패가 같은 회차·같은 직원의 중복 신청 유일 인덱스 위반인지 판정한다.
   *
   * <p>(a) Hibernate가 제약 이름을 얻었으면 그 이름이 {@link #ACTIVE_UNIQUE_INDEX}인지로 판정한다. (b) 이름이 null이면 cause
   * 체인의 {@link SQLException}의 SQLState가 23505(유일 제약 위반)인지로 판정한다. enrollment 테이블의 유일 제약은
   * PK(IDENTITY로 자동 생성되어 직접 값을 넣지 않음)와 이 인덱스뿐이므로 23505는 이 인덱스 위반이다.
   */
  public static boolean isActiveUniqueViolation(DataIntegrityViolationException e) {
    String name = findConstraintName(e);
    if (name != null) {
      return ACTIVE_UNIQUE_INDEX.equalsIgnoreCase(name);
    }
    SQLException sql = findSqlException(e);
    return sql != null && UNIQUE_VIOLATION_SQLSTATE.equals(sql.getSQLState());
  }

  private static String findConstraintName(Throwable e) {
    Throwable t = e;
    for (int i = 0; t != null && i < MAX_DEPTH; i++) {
      if (t instanceof ConstraintViolationException) {
        String name = ((ConstraintViolationException) t).getConstraintName();
        if (name != null) {
          return name;
        }
      }
      t = nextCause(t);
    }
    return null;
  }

  private static SQLException findSqlException(Throwable e) {
    Throwable t = e;
    for (int i = 0; t != null && i < MAX_DEPTH; i++) {
      if (t instanceof SQLException) {
        return (SQLException) t;
      }
      t = nextCause(t);
    }
    return null;
  }

  private static Throwable nextCause(Throwable t) {
    Throwable cause = t.getCause();
    return cause == t ? null : cause;
  }
}
