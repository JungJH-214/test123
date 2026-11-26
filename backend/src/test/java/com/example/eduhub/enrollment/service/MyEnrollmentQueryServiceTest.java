package com.example.eduhub.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.MyEnrollmentResponse;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import com.example.eduhub.enrollment.repository.MyEnrollmentRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyEnrollmentQueryServiceTest {

  @Mock private EnrollmentRepository enrollmentRepository;

  @InjectMocks private MyEnrollmentQueryService service;

  private MyEnrollmentRow row(Long id, EnrollmentStatus status, long preceding) {
    return new MyEnrollmentRow(
        id,
        10L + id,
        "과정" + id,
        LocalDate.of(2026, 12, 1),
        "강의실",
        status,
        LocalDateTime.of(2026, 11, 1, 9, 0),
        preceding);
  }

  @Test
  void findMine_waiting_waitingNumberIsPrecedingPlusOne() {
    when(enrollmentRepository.findMyEnrollments(7L, null))
        .thenReturn(Collections.singletonList(row(1L, EnrollmentStatus.WAITING, 2L)));

    List<MyEnrollmentResponse> result = service.findMine(7L, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getWaitingNumber()).isEqualTo(3);
    assertThat(result.get(0).getCourseTitle()).isEqualTo("과정1");
    assertThat(result.get(0).getSessionId()).isEqualTo(11L);
  }

  @Test
  void findMine_confirmed_waitingNumberIsNull() {
    when(enrollmentRepository.findMyEnrollments(7L, null))
        .thenReturn(Collections.singletonList(row(1L, EnrollmentStatus.CONFIRMED, 5L)));

    List<MyEnrollmentResponse> result = service.findMine(7L, null);

    assertThat(result.get(0).getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(result.get(0).getWaitingNumber()).isNull();
  }

  @Test
  void findMine_passesEmployeeIdAndStatus() {
    when(enrollmentRepository.findMyEnrollments(7L, EnrollmentStatus.WAITING))
        .thenReturn(Collections.emptyList());
    when(enrollmentRepository.findMyEnrollments(7L, null)).thenReturn(Collections.emptyList());

    service.findMine(7L, EnrollmentStatus.WAITING);
    service.findMine(7L, null);

    verify(enrollmentRepository).findMyEnrollments(7L, EnrollmentStatus.WAITING);
    verify(enrollmentRepository).findMyEnrollments(7L, null);
  }

  @Test
  void findMine_keepsRepositoryOrder() {
    when(enrollmentRepository.findMyEnrollments(7L, null))
        .thenReturn(
            Arrays.asList(
                row(3L, EnrollmentStatus.CONFIRMED, 0L),
                row(1L, EnrollmentStatus.WAITING, 0L),
                row(2L, EnrollmentStatus.CONFIRMED, 0L)));

    List<MyEnrollmentResponse> result = service.findMine(7L, null);

    assertThat(result).extracting(MyEnrollmentResponse::getId).containsExactly(3L, 1L, 2L);
  }
}
