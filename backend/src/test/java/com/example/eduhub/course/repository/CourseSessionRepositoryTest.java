package com.example.eduhub.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("pgtest")
class CourseSessionRepositoryTest {

  @Autowired private CourseSessionRepository courseSessionRepository;
  @Autowired private TestEntityManager em;

  @Test
  void findByIdAndCourseId_onlyMatchesOwnCourse() {
    Course java = em.persist(Course.create("Java 기초", "개발", null));
    Course leadership = em.persist(Course.create("리더십", "리더십", null));
    CourseSession session =
        em.persist(CourseSession.create(java, LocalDate.of(2026, 12, 1), "본사", 10));
    em.flush();
    em.clear();

    assertThat(courseSessionRepository.findByIdAndCourseId(session.getId(), java.getId()))
        .isPresent();
    assertThat(courseSessionRepository.findByIdAndCourseId(session.getId(), leadership.getId()))
        .isEmpty();
  }

  @Test
  void update_isFlushedToDatabase() {
    Course course = em.persist(Course.create("Java 기초", "개발", null));
    CourseSession session =
        em.persist(CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", 10));
    em.flush();

    session.update(LocalDate.of(2027, 1, 5), "온라인", 30);
    em.flush();
    em.clear();

    CourseSession found = courseSessionRepository.findById(session.getId()).orElseThrow();
    assertThat(found.getStartDate()).isEqualTo(LocalDate.of(2027, 1, 5));
    assertThat(found.getPlace()).isEqualTo("온라인");
    assertThat(found.getCapacity()).isEqualTo(30);
  }
}
