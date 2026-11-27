package com.example.eduhub.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("pgtest")
class EnrollmentRepositoryTest {

  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private TestEntityManager em;

  private CourseSession newSession(Course course) {
    return em.persist(CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", 10));
  }

  private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 1, 9, 0);

  private Enrollment saveAt(
      CourseSession session, Long employeeId, EnrollmentStatus status, LocalDateTime createdAt) {
    Enrollment e = Enrollment.create(session, employeeId, status);
    ReflectionTestUtils.setField(e, "createdAt", createdAt);
    return em.persist(e);
  }

  private List<Long> ids(List<MyEnrollmentRow> rows) {
    return rows.stream().map(MyEnrollmentRow::getId).collect(Collectors.toList());
  }

  @Test
  void findMyEnrollments_returnsOnlyMine() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    CourseSession b = newSession(course);
    Enrollment mine1 = saveAt(a, 1L, EnrollmentStatus.CONFIRMED, BASE);
    saveAt(a, 2L, EnrollmentStatus.CONFIRMED, BASE.plusMinutes(1));
    Enrollment mine2 = saveAt(b, 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(2));
    saveAt(b, 2L, EnrollmentStatus.WAITING, BASE.plusMinutes(3));
    em.flush();
    em.clear();

    List<MyEnrollmentRow> rows = enrollmentRepository.findMyEnrollments(1L, null);

    assertThat(ids(rows)).containsExactlyInAnyOrder(mine1.getId(), mine2.getId());
  }

  @Test
  void findMyEnrollments_filtersByStatus() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    CourseSession b = newSession(course);
    Enrollment confirmed = saveAt(a, 1L, EnrollmentStatus.CONFIRMED, BASE);
    Enrollment waiting = saveAt(b, 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(1));
    em.flush();
    em.clear();

    assertThat(ids(enrollmentRepository.findMyEnrollments(1L, EnrollmentStatus.WAITING)))
        .containsExactly(waiting.getId());
    assertThat(ids(enrollmentRepository.findMyEnrollments(1L, EnrollmentStatus.CONFIRMED)))
        .containsExactly(confirmed.getId());
    assertThat(ids(enrollmentRepository.findMyEnrollments(1L, null)))
        .containsExactlyInAnyOrder(confirmed.getId(), waiting.getId());
  }

  @Test
  void findMyEnrollments_ordersByCreatedAtDesc() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    Enrollment oldest = saveAt(newSession(course), 1L, EnrollmentStatus.CONFIRMED, BASE);
    Enrollment newest =
        saveAt(newSession(course), 1L, EnrollmentStatus.CONFIRMED, BASE.plusDays(2));
    Enrollment middle =
        saveAt(newSession(course), 1L, EnrollmentStatus.CONFIRMED, BASE.plusDays(1));
    em.flush();
    em.clear();

    assertThat(ids(enrollmentRepository.findMyEnrollments(1L, null)))
        .containsExactly(newest.getId(), middle.getId(), oldest.getId());
  }

  @Test
  void findMyEnrollments_precedingWaitingCountsSameSessionOnly() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    CourseSession b = newSession(course);
    // 임직원 1은 중간(앞 1명, 뒤 1명). 다른 회차 대기자 2명은 먼저 저장해 id가 나보다 작다.
    saveAt(b, 21L, EnrollmentStatus.WAITING, BASE.plusSeconds(30));
    saveAt(b, 22L, EnrollmentStatus.WAITING, BASE.plusSeconds(90));
    saveAt(a, 11L, EnrollmentStatus.WAITING, BASE);
    saveAt(a, 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(1));
    saveAt(a, 12L, EnrollmentStatus.WAITING, BASE.plusMinutes(2));
    em.flush();
    em.clear();

    List<MyEnrollmentRow> rows = enrollmentRepository.findMyEnrollments(1L, null);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getSessionId()).isEqualTo(a.getId());
    assertThat(rows.get(0).getPrecedingWaiting()).isEqualTo(1L);
  }

  @Test
  void findMyEnrollments_precedingWaitingFollowsIdNotCreatedAt() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    // 먼저 저장한 행(id 작음)이 더 늦은 시각이다. 순번은 시각이 아니라 id를 따른다.
    saveAt(a, 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(10));
    saveAt(a, 2L, EnrollmentStatus.WAITING, BASE);
    em.flush();
    em.clear();

    assertThat(enrollmentRepository.findMyEnrollments(1L, null).get(0).getPrecedingWaiting())
        .isEqualTo(0L);
    assertThat(enrollmentRepository.findMyEnrollments(2L, null).get(0).getPrecedingWaiting())
        .isEqualTo(1L);
  }

  @Test
  void findMyEnrollments_sameCreatedAt_ordersByIdDesc() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    Enrollment first = saveAt(newSession(course), 1L, EnrollmentStatus.CONFIRMED, BASE);
    Enrollment second = saveAt(newSession(course), 1L, EnrollmentStatus.CONFIRMED, BASE);
    em.flush();
    em.clear();

    assertThat(ids(enrollmentRepository.findMyEnrollments(1L, null)))
        .containsExactly(second.getId(), first.getId());
  }

  @Test
  void findMyEnrollments_statusFilter_keepsPrecedingWaiting() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    saveAt(a, 2L, EnrollmentStatus.WAITING, BASE);
    saveAt(a, 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(1));
    em.flush();
    em.clear();

    assertThat(
            enrollmentRepository
                .findMyEnrollments(1L, EnrollmentStatus.WAITING)
                .get(0)
                .getPrecedingWaiting())
        .isEqualTo(1L);
    assertThat(enrollmentRepository.findMyEnrollments(1L, null).get(0).getPrecedingWaiting())
        .isEqualTo(1L);
  }

  @Test
  void findMyEnrollments_confirmedNotCountedAsWaiting() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    saveAt(a, 11L, EnrollmentStatus.CONFIRMED, BASE);
    saveAt(a, 12L, EnrollmentStatus.CONFIRMED, BASE.plusMinutes(1));
    saveAt(a, 13L, EnrollmentStatus.WAITING, BASE.plusMinutes(2));
    saveAt(a, 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(3));
    saveAt(a, 14L, EnrollmentStatus.CONFIRMED, BASE.plusMinutes(4)); // 나보다 늦은 확정
    em.flush();
    em.clear();

    List<MyEnrollmentRow> rows = enrollmentRepository.findMyEnrollments(1L, null);

    assertThat(rows.get(0).getPrecedingWaiting()).isEqualTo(1L);
  }

  @Test
  void findMyEnrollments_executesSingleSql() {
    Course c1 = em.persist(Course.create("Java 기초", "개발", null));
    Course c2 = em.persist(Course.create("Spring 심화", "개발", null));
    Course c3 = em.persist(Course.create("SQL", "데이터", null));
    saveAt(newSession(c1), 1L, EnrollmentStatus.CONFIRMED, BASE);
    saveAt(newSession(c2), 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(1));
    saveAt(newSession(c3), 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(2));
    em.flush();
    em.clear();
    Statistics statistics =
        em.getEntityManager()
            .getEntityManagerFactory()
            .unwrap(SessionFactory.class)
            .getStatistics();
    statistics.clear();

    List<MyEnrollmentRow> rows = enrollmentRepository.findMyEnrollments(1L, null);

    // DTO 프로젝션이라 지연 로딩이 없다. 이 테스트는 엔티티 반환으로 바뀌는 퇴행을 막는다.
    assertThat(rows).hasSize(3);
    assertThat(rows.get(0).getCourseTitle()).isNotNull();
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
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
  void findSessionIdByIdAndEmployeeId_owner_returnsSessionId() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    Enrollment e = em.persist(Enrollment.create(a, 1L, EnrollmentStatus.CONFIRMED));
    em.flush();
    em.clear();

    assertThat(enrollmentRepository.findSessionIdByIdAndEmployeeId(e.getId(), 1L))
        .contains(a.getId());
  }

  @Test
  void findSessionIdByIdAndEmployeeId_otherEmployee_empty() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    Enrollment e = em.persist(Enrollment.create(a, 1L, EnrollmentStatus.CONFIRMED));
    em.flush();
    em.clear();

    assertThat(enrollmentRepository.findSessionIdByIdAndEmployeeId(e.getId(), 2L)).isEmpty();
  }

  @Test
  void findFirstBySessionIdAndStatusOrderByIdAsc_returnsSmallestIdWaiting() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    CourseSession b = newSession(course);
    // 다른 회차·다른 상태를 먼저 저장해 더 작은 ID를 갖게 한다. 시각은 ID 순서와 반대로 둔다.
    em.persist(Enrollment.create(b, 9L, EnrollmentStatus.WAITING));
    em.persist(Enrollment.create(a, 8L, EnrollmentStatus.CONFIRMED));
    em.persist(Enrollment.create(a, 7L, EnrollmentStatus.CANCELLED));
    Enrollment first = saveAt(a, 1L, EnrollmentStatus.WAITING, BASE.plusMinutes(10));
    saveAt(a, 2L, EnrollmentStatus.WAITING, BASE);
    saveAt(a, 3L, EnrollmentStatus.WAITING, BASE.plusMinutes(5));
    em.flush();
    em.clear();

    assertThat(
            enrollmentRepository.findFirstBySessionIdAndStatusOrderByIdAsc(
                a.getId(), EnrollmentStatus.WAITING))
        .hasValueSatisfying(e -> assertThat(e.getId()).isEqualTo(first.getId()));
  }

  @Test
  void existsBySessionIdAndEmployeeIdAndStatusNot_cancelledOnly_false() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    em.persist(Enrollment.create(a, 1L, EnrollmentStatus.CANCELLED));
    em.persist(Enrollment.create(a, 2L, EnrollmentStatus.WAITING));
    em.flush();
    em.clear();

    assertThat(
            enrollmentRepository.existsBySessionIdAndEmployeeIdAndStatusNot(
                a.getId(), 1L, EnrollmentStatus.CANCELLED))
        .isFalse();
    assertThat(
            enrollmentRepository.existsBySessionIdAndEmployeeIdAndStatusNot(
                a.getId(), 2L, EnrollmentStatus.CANCELLED))
        .isTrue();
  }

  @Test
  void cancelledThenReenroll_partialUniqueIndexAllows() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession a = newSession(course);
    em.persist(Enrollment.create(a, 1L, EnrollmentStatus.CANCELLED));
    Enrollment again = em.persist(Enrollment.create(a, 1L, EnrollmentStatus.WAITING));

    em.flush();

    assertThat(again.getId()).isNotNull();
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
