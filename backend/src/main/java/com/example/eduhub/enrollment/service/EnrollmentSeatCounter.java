package com.example.eduhub.enrollment.service;

import com.example.eduhub.course.service.SessionSeatCounter;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import com.example.eduhub.enrollment.repository.SessionSeatCountRow;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** {@link SessionSeatCounter} 구현. 확정·수료 신청 수를 회차별로 센다 (D-05). 표시용 조회라 락을 잡지 않는다. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentSeatCounter implements SessionSeatCounter {

  private final EnrollmentRepository enrollmentRepository;

  @Override
  public Map<Long, Long> countSeatsTaken(Collection<Long> sessionIds) {
    if (sessionIds == null || sessionIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<Long, Long> result = new HashMap<>();
    for (SessionSeatCountRow row :
        enrollmentRepository.countSeatsTakenBySessionIds(
            sessionIds, EnrollmentStatus.SEAT_STATUSES)) {
      result.put(row.getSessionId(), row.getSeatsTaken());
    }
    return result;
  }
}
