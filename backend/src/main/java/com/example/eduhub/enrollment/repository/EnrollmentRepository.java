package com.example.eduhub.enrollment.repository;

import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  boolean existsBySessionIdAndEmployeeId(Long sessionId, Long employeeId);

  long countBySessionIdAndStatus(Long sessionId, EnrollmentStatus status);
}
