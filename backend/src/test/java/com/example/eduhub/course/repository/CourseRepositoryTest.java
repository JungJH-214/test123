package com.example.eduhub.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.domain.CourseStatus;
import com.example.eduhub.course.domain.SessionStatus;
import java.time.LocalDate;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("pgtest")
class CourseRepositoryTest {

  @Autowired private CourseRepository courseRepository;
  @Autowired private TestEntityManager em;

  @Test
  void saveAndFind_courseWithSession() {
    Course course = courseRepository.save(Course.create("Java 기초", "개발", "입문 과정"));
    CourseSession session =
        em.persist(CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사 3층", 20));
    em.flush();
    em.clear();

    Course foundCourse = courseRepository.findById(course.getId()).orElseThrow();
    assertThat(foundCourse.getTitle()).isEqualTo("Java 기초");
    assertThat(foundCourse.getCategory()).isEqualTo("개발");
    assertThat(foundCourse.getStatus()).isEqualTo(CourseStatus.OPEN);
    em.clear();

    CourseSession foundSession = em.find(CourseSession.class, session.getId());
    assertThat(Hibernate.isInitialized(foundSession.getCourse())).isFalse();
    assertThat(foundSession.getCourse().getId()).isEqualTo(course.getId());
    assertThat(foundSession.getStartDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    assertThat(foundSession.getCapacity()).isEqualTo(20);
    assertThat(foundSession.getStatus()).isEqualTo(SessionStatus.OPEN);
  }
}
