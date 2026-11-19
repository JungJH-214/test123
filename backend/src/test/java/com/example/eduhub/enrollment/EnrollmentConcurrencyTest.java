package com.example.eduhub.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.repository.CourseRepository;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import com.example.eduhub.enrollment.service.EnrollmentCommandService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * NFR-01 확인용. 로컬 PostgreSQL(pgtest)이 필요하다. CourseSessionRepository.findByIdForUpdate의 @Lock을 주석
 * 처리하면 실패해야 한다.
 */
@SpringBootTest
@ActiveProfiles("pgtest")
class EnrollmentConcurrencyTest {

  private static final int THREADS = 32;
  private static final int APPLICANTS = 100;
  private static final int CAPACITY = 10;

  @Autowired private EnrollmentCommandService enrollmentCommandService;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseSessionRepository courseSessionRepository;

  @AfterEach
  void cleanUp() {
    enrollmentRepository.deleteAllInBatch();
    courseSessionRepository.deleteAllInBatch();
    courseRepository.deleteAllInBatch();
  }

  @Test
  void hundredApplicants_capacityTen_confirmedTenWaitingFive() throws Exception {
    CourseSession session = saveSession();

    Result result = runConcurrently(session.getId(), 1L);

    assertThat(
            enrollmentRepository.countBySessionIdAndStatus(
                session.getId(), EnrollmentStatus.CONFIRMED))
        .isEqualTo(10);
    assertThat(
            enrollmentRepository.countBySessionIdAndStatus(
                session.getId(), EnrollmentStatus.WAITING))
        .isEqualTo(5);
    assertThat(totalInSession(session.getId())).isEqualTo(15);
    assertThat(result.succeeded).isEqualTo(15);
    assertThat(result.rejected).isEqualTo(85);
    assertThat(result.otherErrors).isEmpty();
  }

  @Test
  void nineConfirmed_hundredApplicants_lastSeatOnlyOnce() throws Exception {
    CourseSession session = saveSession();
    for (long employeeId = 1001L; employeeId <= 1009L; employeeId++) {
      enrollmentRepository.save(Enrollment.create(session, employeeId, EnrollmentStatus.CONFIRMED));
    }

    Result result = runConcurrently(session.getId(), 1L);

    assertThat(
            enrollmentRepository.countBySessionIdAndStatus(
                session.getId(), EnrollmentStatus.CONFIRMED))
        .isEqualTo(10);
    assertThat(
            enrollmentRepository.countBySessionIdAndStatus(
                session.getId(), EnrollmentStatus.WAITING))
        .isEqualTo(5);
    assertThat(totalInSession(session.getId())).isEqualTo(15);
    assertThat(result.succeeded).isEqualTo(6);
    assertThat(result.rejected).isEqualTo(94);
    assertThat(result.otherErrors).isEmpty();
  }

  private long totalInSession(Long sessionId) {
    return enrollmentRepository.countBySessionIdAndStatus(sessionId, EnrollmentStatus.CONFIRMED)
        + enrollmentRepository.countBySessionIdAndStatus(sessionId, EnrollmentStatus.WAITING);
  }

  private CourseSession saveSession() {
    Course course = courseRepository.save(Course.create("동시성 과정", "개발", null));
    return courseSessionRepository.save(
        CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", CAPACITY));
  }

  private Result runConcurrently(Long sessionId, long firstEmployeeId) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch ready = new CountDownLatch(APPLICANTS);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(APPLICANTS);
    AtomicInteger succeeded = new AtomicInteger();
    AtomicInteger rejected = new AtomicInteger();
    Queue<Throwable> otherErrors = new ConcurrentLinkedQueue<>();

    boolean finished;
    try {
      for (int i = 0; i < APPLICANTS; i++) {
        long employeeId = firstEmployeeId + i;
        pool.submit(
            () -> {
              try {
                ready.countDown();
                start.await();
                enrollmentCommandService.enroll(sessionId, employeeId);
                succeeded.incrementAndGet();
              } catch (SessionFullException e) {
                rejected.incrementAndGet();
              } catch (Throwable e) {
                otherErrors.add(e);
              } finally {
                done.countDown();
              }
            });
      }
      // 스레드가 32개뿐이라 ready.await()는 쓰지 않는다 (작업 100개가 동시에 대기하지 못한다).
      start.countDown();
      finished = done.await(60, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
      pool.awaitTermination(10, TimeUnit.SECONDS);
    }

    assertThat(finished).as("60초 안에 모든 신청이 끝나야 한다").isTrue();
    return new Result(succeeded.get(), rejected.get(), new ArrayList<>(otherErrors));
  }

  private static class Result {
    private final int succeeded;
    private final int rejected;
    private final List<Throwable> otherErrors;

    private Result(int succeeded, int rejected, List<Throwable> otherErrors) {
      this.succeeded = succeeded;
      this.rejected = rejected;
      this.otherErrors = otherErrors;
    }
  }
}
