package com.example.eduhub.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class EnrollmentConstraintsTest {

  private static DataIntegrityViolationException violation(String sqlState, String constraintName) {
    return new DataIntegrityViolationException(
        "dup",
        new ConstraintViolationException("dup", new SQLException("msg", sqlState), constraintName));
  }

  @Test
  void isActiveUniqueViolation_nameMatches_true() {
    assertThat(
            EnrollmentConstraints.isActiveUniqueViolation(
                violation("23505", "ux_enrollment_session_employee_active")))
        .isTrue();
  }

  @Test
  void isActiveUniqueViolation_nameDiffersInCase_true() {
    assertThat(
            EnrollmentConstraints.isActiveUniqueViolation(
                violation("23505", "UX_ENROLLMENT_SESSION_EMPLOYEE_ACTIVE")))
        .isTrue();
  }

  @Test
  void isActiveUniqueViolation_otherName_false() {
    assertThat(
            EnrollmentConstraints.isActiveUniqueViolation(
                violation("23505", "fk_enrollment_session")))
        .isFalse();
  }

  @Test
  void isActiveUniqueViolation_nullNameAndSqlState23505_true() {
    assertThat(EnrollmentConstraints.isActiveUniqueViolation(violation("23505", null))).isTrue();
  }

  @Test
  void isActiveUniqueViolation_nullNameAndSqlState23503_false() {
    assertThat(EnrollmentConstraints.isActiveUniqueViolation(violation("23503", null))).isFalse();
  }

  @Test
  void isActiveUniqueViolation_noCause_false() {
    assertThat(
            EnrollmentConstraints.isActiveUniqueViolation(new DataIntegrityViolationException("x")))
        .isFalse();
  }
}
