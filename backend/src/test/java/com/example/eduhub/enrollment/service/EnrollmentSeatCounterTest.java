package com.example.eduhub.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import com.example.eduhub.enrollment.repository.SessionSeatCountRow;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnrollmentSeatCounterTest {

  private final EnrollmentRepository repository = mock(EnrollmentRepository.class);
  private final EnrollmentSeatCounter counter = new EnrollmentSeatCounter(repository);

  private static SessionSeatCountRow row(long sessionId, long seatsTaken) {
    SessionSeatCountRow row = mock(SessionSeatCountRow.class);
    when(row.getSessionId()).thenReturn(sessionId);
    when(row.getSeatsTaken()).thenReturn(seatsTaken);
    return row;
  }

  @Test
  void countSeatsTaken_emptyInput_returnsEmptyMapWithoutQuery() {
    Map<Long, Long> result = counter.countSeatsTaken(Collections.<Long>emptyList());

    assertThat(result).isEmpty();
    verifyNoInteractions(repository);
  }

  @Test
  void countSeatsTaken_rows_becomeMapUsingSeatStatuses() {
    List<SessionSeatCountRow> rows = Arrays.asList(row(1L, 3L), row(2L, 5L));
    when(repository.countSeatsTakenBySessionIds(any(), any())).thenReturn(rows);

    Map<Long, Long> result = counter.countSeatsTaken(Arrays.asList(1L, 2L, 3L));

    assertThat(result).hasSize(2).containsEntry(1L, 3L).containsEntry(2L, 5L);
    verify(repository)
        .countSeatsTakenBySessionIds(Arrays.asList(1L, 2L, 3L), EnrollmentStatus.SEAT_STATUSES);
  }
}
