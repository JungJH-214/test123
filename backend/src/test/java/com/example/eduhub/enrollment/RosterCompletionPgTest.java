package com.example.eduhub.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.repository.CourseRepository;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.CompletionResponse;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import com.example.eduhub.enrollment.service.EnrollmentCommandService;
import com.example.eduhub.enrollment.service.RosterService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** FR-08 수료 처리의 실제 DB 확인(W-3, W-4). 로컬 PostgreSQL(pgtest)이 필요하다. */
@SpringBootTest
@ActiveProfiles("pgtest")
class RosterCompletionPgTest {

  private static final int THREADS = 32;
  private static final int CAPACITY = 10;

  @Autowired private RosterService rosterService;
  @Autowired private EnrollmentCommandService enrollmentCommandService;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseSessionRepository courseSessionRepository;
  @PersistenceContext private EntityManager entityManager;

  @AfterEach
  void cleanUp() {
    enrollmentRepository.deleteAllInBatch();
    courseSessionRepository.deleteAllInBatch();
    courseRepository.deleteAllInBatch();
  }

  /** W-3: 같은 임직원의 취소 건(작은 ID)과 재신청 건(큰 ID)이 있으면 재신청 건이 수료된다. */
  @Test
  void complete_cancelledThenReenrolled_completesLatestOnly() {
    CourseSession session = saveStartedSession();
    Enrollment cancelled =
        enrollmentRepository.saveAndFlush(
            Enrollment.create(session, 1L, EnrollmentStatus.CANCELLED));
    Enrollment reenrolled =
        enrollmentRepository.saveAndFlush(
            Enrollment.create(session, 1L, EnrollmentStatus.CONFIRMED));
    assertThat(reenrolled.getId()).isGreaterThan(cancelled.getId());

    CompletionResponse response =
        rosterService.complete(session.getId(), Collections.singletonList(1L));

    assertThat(response.getCompleted()).containsExactly(1L);
    assertThat(response.getFailed()).isEmpty();
    assertThat(statusOf(reenrolled.getId())).isEqualTo(EnrollmentStatus.COMPLETED);
    assertThat(statusOf(cancelled.getId())).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  /** W-4: 신청 30건과 확정 5명의 수료를 동시에 실행해도 확정+수료 합계는 정원과 같고 대기는 상한 이하다. */
  @Test
  void enrollAndComplete_concurrently_seatsTotalEqualsCapacity() throws Exception {
    CourseSession session = saveStartedSession();
    Long sessionId = session.getId();
    List<Long> confirmedEmployees = new ArrayList<>();
    for (long employeeId = 1L; employeeId <= 5L; employeeId++) {
      enrollmentRepository.save(Enrollment.create(session, employeeId, EnrollmentStatus.CONFIRMED));
      confirmedEmployees.add(employeeId);
    }

    List<Callable<Object>> tasks = new ArrayList<>();
    tasks.add(() -> rosterService.complete(sessionId, confirmedEmployees));
    for (long employeeId = 101L; employeeId < 131L; employeeId++) {
      long id = employeeId;
      tasks.add(() -> enrollmentCommandService.enroll(sessionId, id));
    }
    List<Throwable> errors = runConcurrently(tasks);

    // 정원이 차면 SessionFullException(409)이 나는 것은 정상이다.
    assertThat(
            errors.stream()
                .filter(e -> !(e instanceof SessionFullException))
                .collect(Collectors.toList()))
        .isEmpty();
    long confirmed = count(sessionId, EnrollmentStatus.CONFIRMED);
    long completed = count(sessionId, EnrollmentStatus.COMPLETED);
    long waiting = count(sessionId, EnrollmentStatus.WAITING);
    assertThat(confirmed + completed).isEqualTo(CAPACITY);
    assertThat(completed).isEqualTo(5);
    assertThat(waiting).isLessThanOrEqualTo(CAPACITY / 2);
    for (long employeeId = 1L; employeeId <= 5L; employeeId++) {
      assertThat(statusOfEmployee(sessionId, employeeId)).isEqualTo(EnrollmentStatus.COMPLETED);
    }
  }

  private CourseSession saveStartedSession() {
    Course course = courseRepository.save(Course.create("수료 pgtest 과정", "개발", null));
    LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    return courseSessionRepository.save(CourseSession.create(course, startDate, "본사", CAPACITY));
  }

  private long count(Long sessionId, EnrollmentStatus status) {
    return enrollmentRepository.countBySessionIdAndStatus(sessionId, status);
  }

  private EnrollmentStatus statusOf(Long enrollmentId) {
    return entityManager
        .createQuery("select e.status from Enrollment e where e.id = :id", EnrollmentStatus.class)
        .setParameter("id", enrollmentId)
        .getSingleResult();
  }

  private EnrollmentStatus statusOfEmployee(Long sessionId, Long employeeId) {
    return entityManager
        .createQuery(
            "select e.status from Enrollment e where e.session.id = :sid"
                + " and e.employeeId = :eid",
            EnrollmentStatus.class)
        .setParameter("sid", sessionId)
        .setParameter("eid", employeeId)
        .getSingleResult();
  }

  private List<Throwable> runConcurrently(List<Callable<Object>> tasks) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(tasks.size());
    Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
    boolean finished;
    try {
      for (Callable<Object> task : tasks) {
        pool.submit(
            () -> {
              try {
                start.await();
                task.call();
              } catch (Throwable e) {
                errors.add(e);
              } finally {
                done.countDown();
              }
            });
      }
      start.countDown();
      finished = done.await(60, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
      pool.awaitTermination(10, TimeUnit.SECONDS);
    }
    assertThat(finished).as("60초 안에 모든 요청이 끝나야 한다").isTrue();
    return new ArrayList<>(errors);
  }
}
