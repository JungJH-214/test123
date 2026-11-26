package com.example.eduhub.enrollment.service;

import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.MyEnrollmentResponse;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 신청 내역 조회 (FR-07, D-04). 규칙: 로그인한 임직원의 신청만 최근 신청순으로 돌려준다. 상태를 주면 그 상태만 돌려준다. 대기 순번은 저장하지 않고 같은
 * 회차의 대기 신청 사이에서 신청 ID 순 (D-04)으로 계산한다. 과정·회차·대기 순번은 쿼리 하나로 가져온다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyEnrollmentQueryService {

  private final EnrollmentRepository enrollmentRepository;

  public List<MyEnrollmentResponse> findMine(Long employeeId, EnrollmentStatus status) {
    return enrollmentRepository.findMyEnrollments(employeeId, status).stream()
        .map(MyEnrollmentResponse::from)
        .collect(Collectors.toList());
  }
}
