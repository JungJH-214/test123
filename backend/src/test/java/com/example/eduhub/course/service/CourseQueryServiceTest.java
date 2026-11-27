package com.example.eduhub.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.dto.CourseDetailResponse;
import com.example.eduhub.course.dto.SessionResponse;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class CourseQueryServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 12, 1);

  @Autowired private CourseQueryService service;
  @Autowired private EntityManager em;
  @Autowired private EntityManagerFactory emf;

  @Test
  void getDetail_remainingSeatsPerSession_countsConfirmedAndCompletedOnly() {
    Course course = persistCourse();
    CourseSession s1 = persistSession(course, DATE, 10);
    CourseSession s2 = persistSession(course, DATE.plusDays(1), 5);
    CourseSession s3 = persistSession(course, DATE.plusDays(2), 8);
    CourseSession s4 = persistSession(course, DATE.plusDays(3), 3);
    enroll(s1, EnrollmentStatus.CONFIRMED, 1L);
    enroll(s1, EnrollmentStatus.CONFIRMED, 2L);
    enroll(s1, EnrollmentStatus.COMPLETED, 3L);
    enroll(s2, EnrollmentStatus.WAITING, 1L);
    enroll(s2, EnrollmentStatus.CANCELLED, 2L);
    for (long i = 1; i <= 4; i++) { // 정원 3에 4개: 초과해도 0
      enroll(s4, EnrollmentStatus.CONFIRMED, i);
    }
    em.flush();
    em.clear();

    CourseDetailResponse detail = service.getDetail(course.getId());

    List<SessionResponse> sessions = detail.getSessions();
    assertThat(sessions)
        .extracting(SessionResponse::getId)
        .containsExactly(s1.getId(), s2.getId(), s3.getId(), s4.getId());
    assertThat(sessions).extracting(SessionResponse::getRemainingSeats).containsExactly(7, 5, 8, 0);
  }

  @Test
  void getDetail_sqlCountIsThree_regardlessOfSessionCount() {
    Course many = persistCourse();
    for (int i = 0; i < 3; i++) {
      CourseSession s = persistSession(many, DATE.plusDays(i), 10);
      enroll(s, EnrollmentStatus.CONFIRMED, 1L);
    }
    Course single = persistCourse();
    enroll(persistSession(single, DATE, 10), EnrollmentStatus.CONFIRMED, 1L);
    em.flush();
    em.clear();
    Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();

    statistics.clear();
    service.getDetail(many.getId());
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);

    em.clear();
    statistics.clear();
    service.getDetail(single.getId());
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
  }

  private Course persistCourse() {
    Course course = Course.create("Java 기초", "개발", null);
    em.persist(course);
    return course;
  }

  private CourseSession persistSession(Course course, LocalDate date, int capacity) {
    CourseSession session = CourseSession.create(course, date, "본사", capacity);
    em.persist(session);
    return session;
  }

  private void enroll(CourseSession session, EnrollmentStatus status, long employeeId) {
    em.persist(Enrollment.create(session, employeeId, status));
  }
}
