package com.example.eduhub.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.repository.CourseRepository;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.EnrollmentResponse;
import com.example.eduhub.enrollment.exception.EnrollmentAlreadyCancelledException;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import com.example.eduhub.enrollment.service.EnrollmentCommandService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** FR-06(NFR-01) 취소·승급 동시성 확인용. 로컬 PostgreSQL(pgtest)이 필요하다. 공유 DB이므로 이 테스트가 만든 회차 기준으로만 정리한다. */
@SpringBootTest
@ActiveProfiles("pgtest")
class EnrollmentCancelConcurrencyTest {

  private static final int THREADS = 32;
  private static final int CAPACITY = 10;
  private static final int WAITING = 5;

  @Autowired private EnrollmentCommandService enrollmentCommandService;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseSessionRepository courseSessionRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @PersistenceContext private EntityManager entityManager;

  private final List<Long> sessionIds = new ArrayList<>();
  private final List<Long> courseIds = new ArrayList<>();

  @AfterEach
  void cleanUp() {
    if (sessionIds.isEmpty() && courseIds.isEmpty()) {
      return;
    }
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              if (!sessionIds.isEmpty()) {
                entityManager
                    .createQuery("delete from Enrollment e where e.session.id in :ids")
                    .setParameter("ids", sessionIds)
                    .executeUpdate();
                entityManager
                    .createQuery("delete from CourseSession s where s.id in :ids")
                    .setParameter("ids", sessionIds)
                    .executeUpdate();
              }
              if (!courseIds.isEmpty()) {
                entityManager
                    .createQuery("delete from Course c where c.id in :ids")
                    .setParameter("ids", courseIds)
                    .executeUpdate();
              }
            });
  }

  @Test
  void cancelOneConfirmed_whileFiftyEnroll_firstWaitingPromoted_capacityKept() throws Exception {
    Long sessionId = saveSession();
    Map<Long, Long> ids = prepare(sessionId);

    List<Callable<Object>> tasks = new ArrayList<>();
    tasks.add(() -> enrollmentCommandService.cancel(ids.get(1L), 1L));
    for (long employeeId = 101L; employeeId <= 150L; employeeId++) {
      long id = employeeId;
      tasks.add(() -> enrollmentCommandService.enroll(sessionId, id));
    }
    Outcome outcome = runConcurrently(tasks);

    List<Throwable> unexpected =
        outcome.errors.stream()
            .filter(e -> !(e instanceof SessionFullException))
            .collect(Collectors.toList());
    assertThat(unexpected).isEmpty();
    long confirmed = count(sessionId, EnrollmentStatus.CONFIRMED);
    long waiting = count(sessionId, EnrollmentStatus.WAITING);
    int newSucceeded = outcome.succeeded - 1; // 전체 성공에서 취소 1건을 뺀다
    assertThat(confirmed).isEqualTo(10);
    assertThat(waiting).isBetween(4L, 5L);
    assertThat(newSucceeded).isEqualTo((int) (waiting - 4));
    assertThat(confirmed + waiting).isEqualTo(14 + newSucceeded);
    assertThat(statusOf(ids.get(1L))).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(statusOf(ids.get(11L))).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(confirmedAmong(sessionId, 101L, 150L)).isZero();
    // D-04: 신규 대기자는 기존 마지막 대기자(사원 15)보다 뒤 순번(큰 ID)
    assertThat(waitingIdsAmong(sessionId, 101L, 150L)).allMatch(id -> id > ids.get(15L));
  }

  /**
   * R5(Q5): 정원만 늘어 빈 자리 2 + 대기 5인 상태에서 신규 50명이 동시에 신청해도, 대기자(사원 11, 12)가 먼저 확정된다. enroll()에서
   * fillSeatsFromWaiting 호출을 지우면 첫 신규 신청자가 확정되므로 이 테스트는 실패해야 한다.
   */
  @Test
  void freeSeatsAfterCapacityIncrease_waitingPromotedBeforeNewApplicants() throws Exception {
    Long sessionId = saveSession();
    Map<Long, Long> ids = prepare(sessionId);

    // 회차 수정 API(updateSession)를 거치지 않고, 승급 없이 빈 자리만 만들기 위해 정원을 직접 바꾼다.
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status ->
                entityManager
                    .createQuery("update CourseSession s set s.capacity = 12 where s.id = :id")
                    .setParameter("id", sessionId)
                    .executeUpdate());

    List<Callable<Object>> tasks = new ArrayList<>();
    for (long employeeId = 101L; employeeId <= 150L; employeeId++) {
      long id = employeeId;
      tasks.add(() -> enrollmentCommandService.enroll(sessionId, id));
    }
    Outcome outcome = runConcurrently(tasks);

    List<Throwable> unexpected =
        outcome.errors.stream()
            .filter(e -> !(e instanceof SessionFullException))
            .collect(Collectors.toList());
    assertThat(unexpected).isEmpty();
    assertThat(statusOf(ids.get(11L))).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(statusOf(ids.get(12L))).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(confirmedAmong(sessionId, 101L, 150L)).isZero();
    assertThat(count(sessionId, EnrollmentStatus.CONFIRMED)).isEqualTo(12);
    assertThat(count(sessionId, EnrollmentStatus.WAITING)).isEqualTo(6); // 상한 12 / 2
    assertThat(outcome.succeeded).isEqualTo(3);
    assertThat(outcome.errors).hasSize(47);
    assertThat(waitingIdsAmong(sessionId, 101L, 150L)).hasSize(3).allMatch(id -> id > ids.get(15L));
  }

  /**
   * W1: 정원만 늘어 빈 자리 2 + 대기 5인 상태에서 대기자(사원 15)가 취소해도 대기 1순위(사원 11, 12)가 승급한다. cancel()에서 대기 취소 때
   * fillSeatsFromWaiting 호출을 빼면 이 테스트는 실패해야 한다.
   */
  @Test
  void cancelWaiting_withFreeSeats_promotesWaitingInOrder() {
    Long sessionId = saveSession();
    Map<Long, Long> ids = prepare(sessionId);
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status ->
                entityManager
                    .createQuery("update CourseSession s set s.capacity = 12 where s.id = :id")
                    .setParameter("id", sessionId)
                    .executeUpdate());

    enrollmentCommandService.cancel(ids.get(15L), 15L);

    assertThat(statusOf(ids.get(15L))).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(statusOf(ids.get(11L))).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(statusOf(ids.get(12L))).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(statusOf(ids.get(13L))).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(statusOf(ids.get(14L))).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(count(sessionId, EnrollmentStatus.CONFIRMED)).isEqualTo(12);
    assertThat(count(sessionId, EnrollmentStatus.WAITING)).isEqualTo(2);
  }

  @Test
  void sameEnrollmentCancelledTenTimes_onlyOneSucceeds_onePromotion() throws Exception {
    Long sessionId = saveSession();
    Map<Long, Long> ids = prepare(sessionId);

    List<Callable<Object>> tasks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      tasks.add(() -> enrollmentCommandService.cancel(ids.get(1L), 1L));
    }
    Outcome outcome = runConcurrently(tasks);

    assertThat(outcome.succeeded).isEqualTo(1);
    assertThat(outcome.errors)
        .hasSize(9)
        .allMatch(e -> e instanceof EnrollmentAlreadyCancelledException);
    assertThat(statusOf(ids.get(1L))).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(statusOf(ids.get(11L))).isEqualTo(EnrollmentStatus.CONFIRMED);
    for (long employeeId = 12L; employeeId <= 15L; employeeId++) {
      assertThat(statusOf(ids.get(employeeId))).isEqualTo(EnrollmentStatus.WAITING);
    }
    assertThat(count(sessionId, EnrollmentStatus.CONFIRMED)).isEqualTo(10);
    assertThat(count(sessionId, EnrollmentStatus.WAITING)).isEqualTo(4);
  }

  // 최종 상태만 단언한다. 승급 순서(ID 순)는 R1·R2·R4와 T1의 EnrollmentRepositoryTest가 맡는다.
  @Test
  void allConfirmedCancelConcurrently_allWaitingPromoted() throws Exception {
    Long sessionId = saveSession();
    Map<Long, Long> ids = prepare(sessionId);

    List<Callable<Object>> tasks = new ArrayList<>();
    for (long employeeId = 1L; employeeId <= 10L; employeeId++) {
      long id = employeeId;
      tasks.add(() -> enrollmentCommandService.cancel(ids.get(id), id));
    }
    Outcome outcome = runConcurrently(tasks);

    assertThat(outcome.errors).isEmpty();
    assertThat(outcome.succeeded).isEqualTo(10);
    for (long employeeId = 11L; employeeId <= 15L; employeeId++) {
      assertThat(statusOf(ids.get(employeeId))).isEqualTo(EnrollmentStatus.CONFIRMED);
    }
    assertThat(count(sessionId, EnrollmentStatus.CONFIRMED)).isEqualTo(5);
    assertThat(count(sessionId, EnrollmentStatus.WAITING)).isZero();
  }

  @Test
  void cancelThenReenroll_newIdBehindExistingWaiting() {
    Long sessionId = saveSession();
    Map<Long, Long> ids = prepare(sessionId);

    enrollmentCommandService.cancel(ids.get(1L), 1L);
    assertThat(statusOf(ids.get(11L))).isEqualTo(EnrollmentStatus.CONFIRMED);
    long waitingBefore = count(sessionId, EnrollmentStatus.WAITING);

    EnrollmentResponse again = enrollmentCommandService.enroll(sessionId, 1L);

    assertThat(again.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(again.getId()).isNotEqualTo(ids.get(1L));
    assertThat(again.getWaitingNumber()).isEqualTo((int) waitingBefore + 1);
  }

  /** 정원 10, 확정 10(사원 1~10), 대기 5(사원 11~15)를 순서대로 신청해 만든다. 사원 ID별 신청 ID를 돌려준다. */
  private Map<Long, Long> prepare(Long sessionId) {
    Map<Long, Long> ids = new HashMap<>();
    for (long employeeId = 1L; employeeId <= CAPACITY + WAITING; employeeId++) {
      EnrollmentResponse response = enrollmentCommandService.enroll(sessionId, employeeId);
      ids.put(employeeId, response.getId());
    }
    assertThat(count(sessionId, EnrollmentStatus.CONFIRMED)).isEqualTo(CAPACITY);
    assertThat(count(sessionId, EnrollmentStatus.WAITING)).isEqualTo(WAITING);
    return ids;
  }

  private Long saveSession() {
    Course course = courseRepository.save(Course.create("취소 동시성 과정", "개발", null));
    courseIds.add(course.getId());
    LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(30);
    CourseSession session =
        courseSessionRepository.save(CourseSession.create(course, startDate, "본사", CAPACITY));
    sessionIds.add(session.getId());
    return session.getId();
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

  private long confirmedAmong(Long sessionId, long fromEmployeeId, long toEmployeeId) {
    return entityManager
        .createQuery(
            "select count(e) from Enrollment e where e.session.id = :sid and e.status = :st"
                + " and e.employeeId between :from and :to",
            Long.class)
        .setParameter("sid", sessionId)
        .setParameter("st", EnrollmentStatus.CONFIRMED)
        .setParameter("from", fromEmployeeId)
        .setParameter("to", toEmployeeId)
        .getSingleResult();
  }

  private List<Long> waitingIdsAmong(Long sessionId, long fromEmployeeId, long toEmployeeId) {
    return entityManager
        .createQuery(
            "select e.id from Enrollment e where e.session.id = :sid and e.status = :st"
                + " and e.employeeId between :from and :to",
            Long.class)
        .setParameter("sid", sessionId)
        .setParameter("st", EnrollmentStatus.WAITING)
        .setParameter("from", fromEmployeeId)
        .setParameter("to", toEmployeeId)
        .getResultList();
  }

  private Outcome runConcurrently(List<Callable<Object>> tasks) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(tasks.size());
    Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
    java.util.concurrent.atomic.AtomicInteger succeeded =
        new java.util.concurrent.atomic.AtomicInteger();

    boolean finished;
    try {
      for (Callable<Object> task : tasks) {
        pool.submit(
            () -> {
              try {
                start.await();
                task.call();
                succeeded.incrementAndGet();
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
    return new Outcome(succeeded.get(), new ArrayList<>(errors));
  }

  private static class Outcome {
    private final int succeeded;
    private final List<Throwable> errors;

    private Outcome(int succeeded, List<Throwable> errors) {
      this.succeeded = succeeded;
      this.errors = errors;
    }
  }
}
