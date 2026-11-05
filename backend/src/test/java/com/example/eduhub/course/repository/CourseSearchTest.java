package com.example.eduhub.course.repository;

import static com.example.eduhub.course.repository.CourseSpecifications.categoryEquals;
import static com.example.eduhub.course.repository.CourseSpecifications.titleContains;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("pgtest")
class CourseSearchTest {

  private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20, Sort.by("id").descending());

  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseSessionRepository courseSessionRepository;
  @Autowired private TestEntityManager em;

  private Course javaBasic;

  @BeforeEach
  void setUp() {
    courseSessionRepository.deleteAllInBatch();
    courseRepository.deleteAllInBatch();
    javaBasic = courseRepository.save(Course.create("Java 기초", "개발", null));
    courseRepository.save(Course.create("Spring 심화", "개발", null));
    courseRepository.save(Course.create("리더십 입문", "리더십", null));
    courseRepository.save(Course.create("Java로 보는 리더십", "리더십", null));
    em.flush();
    em.clear();
  }

  @Test
  void search_keywordMatchesTitleIgnoringCase() {
    assertThat(titles(search(titleContains("java"), FIRST_PAGE)))
        .containsExactlyInAnyOrder("Java 기초", "Java로 보는 리더십");
  }

  @Test
  void search_categoryMatchesExactly() {
    assertThat(titles(search(categoryEquals("리더십"), FIRST_PAGE)))
        .containsExactlyInAnyOrder("리더십 입문", "Java로 보는 리더십");
  }

  @Test
  void search_keywordAndCategoryBothApply() {
    Specification<Course> spec =
        Specification.where(titleContains("java")).and(categoryEquals("개발"));

    assertThat(titles(search(spec, FIRST_PAGE))).containsExactly("Java 기초");
  }

  @Test
  void search_noCondition_returnsPagedAll() {
    Page<Course> page = search(null, PageRequest.of(0, 2, Sort.by("id").descending()));

    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(4);
    assertThat(page.getContent().get(0).getTitle()).isEqualTo("Java로 보는 리더십");
  }

  @Test
  void search_blankKeywordIsIgnored() {
    Specification<Course> spec = Specification.where(titleContains("  ")).and(categoryEquals(""));

    assertThat(search(spec, FIRST_PAGE).getTotalElements()).isEqualTo(4);
  }

  @Test
  void findSessions_onlyThatCourse_orderedByStartDate() {
    Course course = courseRepository.findById(javaBasic.getId()).orElseThrow();
    Course other = courseRepository.save(Course.create("다른 과정", "개발", null));
    em.persist(CourseSession.create(course, LocalDate.of(2026, 12, 10), "본사 3층", 20));
    em.persist(CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사 3층", 20));
    em.persist(CourseSession.create(other, LocalDate.of(2026, 11, 20), "온라인", 50));
    em.flush();
    em.clear();

    List<CourseSession> sessions =
        courseSessionRepository.findByCourseIdOrderByStartDateAsc(course.getId());

    assertThat(sessions)
        .extracting(CourseSession::getStartDate)
        .containsExactly(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10));
  }

  private Page<Course> search(Specification<Course> spec, PageRequest pageable) {
    return courseRepository.findAll(Specification.where(spec), pageable);
  }

  private List<String> titles(Page<Course> page) {
    return page.getContent().stream().map(Course::getTitle).collect(Collectors.toList());
  }
}
