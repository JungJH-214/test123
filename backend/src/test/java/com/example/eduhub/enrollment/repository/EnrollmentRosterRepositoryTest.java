package com.example.eduhub.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;
import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** 명단 조회 쿼리 (FR-08). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("pgtest")
class EnrollmentRosterRepositoryTest {

  private static final Long MISSING_EMPLOYEE_ID = 900_000_000_000L;

  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private TestEntityManager em;

  private CourseSession newSession() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    return em.persist(CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", 10));
  }

  private Employee newEmployee(String no, String name) {
    return em.persist(Employee.create(no, name, "hash", Role.EMPLOYEE));
  }

  @Test
  void findRosterRows_ordersByEnrollmentIdAndJoinsEmployee() {
    CourseSession session = newSession();
    Employee e1 = newEmployee("R-0001", "김하나");
    Employee e2 = newEmployee("R-0002", "이둘");
    Employee e3 = newEmployee("R-0003", "박셋");
    Enrollment first = em.persist(Enrollment.create(session, e3.getId(), EnrollmentStatus.WAITING));
    Enrollment second =
        em.persist(Enrollment.create(session, e1.getId(), EnrollmentStatus.CONFIRMED));
    Enrollment third =
        em.persist(Enrollment.create(session, e2.getId(), EnrollmentStatus.COMPLETED));
    em.flush();
    em.clear();

    List<RosterRow> rows = enrollmentRepository.findRosterRows(session.getId());

    assertThat(rows)
        .extracting(RosterRow::getEnrollmentId)
        .containsExactly(first.getId(), second.getId(), third.getId());
    RosterRow row = rows.get(1);
    assertThat(row.getEmployeeId()).isEqualTo(e1.getId());
    assertThat(row.getEmployeeNo()).isEqualTo("R-0001");
    assertThat(row.getName()).isEqualTo("김하나");
    assertThat(row.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(row.getCreatedAt()).isNotNull();
  }

  @Test
  void findRosterRows_excludesOtherSessions() {
    CourseSession a = newSession();
    CourseSession b = newSession();
    Employee e1 = newEmployee("R-0011", "김하나");
    Employee e2 = newEmployee("R-0012", "이둘");
    em.persist(Enrollment.create(a, e1.getId(), EnrollmentStatus.CONFIRMED));
    em.persist(Enrollment.create(b, e2.getId(), EnrollmentStatus.CONFIRMED));
    em.flush();
    em.clear();

    List<RosterRow> rows = enrollmentRepository.findRosterRows(a.getId());

    assertThat(rows.stream().map(RosterRow::getEmployeeId).collect(Collectors.toList()))
        .containsExactly(e1.getId());
  }

  @Test
  void findRosterRows_withoutEmployeeRow_keepsEntryWithNullNoAndName() {
    CourseSession session = newSession();
    em.persist(Enrollment.create(session, MISSING_EMPLOYEE_ID, EnrollmentStatus.CONFIRMED));
    em.flush();
    em.clear();

    List<RosterRow> rows = enrollmentRepository.findRosterRows(session.getId());

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getEmployeeId()).isEqualTo(MISSING_EMPLOYEE_ID);
    assertThat(rows.get(0).getEmployeeNo()).isNull();
    assertThat(rows.get(0).getName()).isNull();
  }

  @Test
  void findRosterRows_noEnrollments_returnsEmpty() {
    CourseSession session = newSession();
    em.flush();
    em.clear();

    assertThat(enrollmentRepository.findRosterRows(session.getId())).isEmpty();
  }

  @Test
  void findBySessionIdAndEmployeeIdIn_returnsOnlyMatchingEmployeesInSession() {
    CourseSession a = newSession();
    CourseSession b = newSession();
    Employee e1 = newEmployee("R-0021", "김하나");
    Employee e2 = newEmployee("R-0022", "이둘");
    Employee e3 = newEmployee("R-0023", "박셋");
    Enrollment inA1 = em.persist(Enrollment.create(a, e1.getId(), EnrollmentStatus.CONFIRMED));
    em.persist(Enrollment.create(a, e3.getId(), EnrollmentStatus.WAITING));
    em.persist(Enrollment.create(b, e2.getId(), EnrollmentStatus.CONFIRMED));
    em.flush();
    em.clear();

    List<Enrollment> found =
        enrollmentRepository.findBySessionIdAndEmployeeIdIn(
            a.getId(), Arrays.asList(e1.getId(), e2.getId(), MISSING_EMPLOYEE_ID));

    assertThat(found).extracting(Enrollment::getId).containsExactly(inA1.getId());
  }
}
