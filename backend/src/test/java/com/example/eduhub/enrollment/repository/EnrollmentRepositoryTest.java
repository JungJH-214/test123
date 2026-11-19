package com.example.eduhub.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("pgtest")
class EnrollmentRepositoryTest {

  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private TestEntityManager em;

  private CourseSession newSession(Course course) {
    return em.persist(CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", 10));
  }

  @Test
  void countBySessionIdAndStatus_countsOnlyGivenStatusAndSession() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    CourseSession b = newSession(course);
    em.persist(Enrollment.create(a, 1L, EnrollmentStatus.CONFIRMED));
    em.persist(Enrollment.create(a, 2L, EnrollmentStatus.CONFIRMED));
    em.persist(Enrollment.create(a, 3L, EnrollmentStatus.WAITING));
    em.persist(Enrollment.create(b, 4L, EnrollmentStatus.CONFIRMED));
    em.flush();
    em.clear();

    assertThat(
            enrollmentRepository.countBySessionIdAndStatus(a.getId(), EnrollmentStatus.CONFIRMED))
        .isEqualTo(2);
    assertThat(enrollmentRepository.countBySessionIdAndStatus(a.getId(), EnrollmentStatus.WAITING))
        .isEqualTo(1);
  }

  @Test
  void existsBySessionIdAndEmployeeId_trueOnlyForSameSessionAndEmployee() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    CourseSession b = newSession(course);
    em.persist(Enrollment.create(a, 1L, EnrollmentStatus.CONFIRMED));
    em.flush();
    em.clear();

    assertThat(enrollmentRepository.existsBySessionIdAndEmployeeId(a.getId(), 1L)).isTrue();
    assertThat(enrollmentRepository.existsBySessionIdAndEmployeeId(a.getId(), 2L)).isFalse();
    assertThat(enrollmentRepository.existsBySessionIdAndEmployeeId(b.getId(), 1L)).isFalse();
  }

  @Test
  void saveAndFlush_sameSessionAndEmployee_throwsDataIntegrityViolation() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    em.flush();
    em.clear();
    CourseSession session = em.find(CourseSession.class, a.getId());

    enrollmentRepository.saveAndFlush(Enrollment.create(session, 1L, EnrollmentStatus.CONFIRMED));

    assertThatThrownBy(
            () ->
                enrollmentRepository.saveAndFlush(
                    Enrollment.create(session, 1L, EnrollmentStatus.WAITING)))
        .isInstanceOfSatisfying(
            DataIntegrityViolationException.class,
            e -> assertThat(EnrollmentConstraints.isActiveUniqueViolation(e)).isTrue());
  }
}
