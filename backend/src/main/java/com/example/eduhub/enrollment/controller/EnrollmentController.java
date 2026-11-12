package com.example.eduhub.enrollment.controller;

import com.example.eduhub.common.error.ErrorResponse;
import com.example.eduhub.enrollment.dto.EnrollmentResponse;
import com.example.eduhub.enrollment.service.EnrollmentCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회차 수강 신청 (FR-04, NFR-01).
 *
 * <p>TODO(NFR-03): 인증이 아직 없어 신청자를 X-Employee-Id 헤더로 받는다. 인증을 넣으면 로그인 사용자에서 employeeId를 꺼내고 이 헤더를
 * 지운다.
 */
@Tag(name = "Enrollment", description = "수강 신청")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class EnrollmentController {

  static final String EMPLOYEE_ID_HEADER = "X-Employee-Id";

  private final EnrollmentCommandService enrollmentCommandService;

  @Operation(summary = "수강 신청 (정원 안이면 확정, 정원이 차면 대기)")
  @ApiResponse(responseCode = "201", description = "신청 성공 (status: CONFIRMED 또는 WAITING)")
  @ApiResponse(
      responseCode = "400",
      description = "신청자 헤더 없음 또는 형식 오류 (MISSING_EMPLOYEE_ID), 경로 값 형식 오류 (INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "회차가 없음 (SESSION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "종료된 회차, 중복 신청, 정원·대기 마감 (SESSION_CLOSED, ENROLLMENT_DUPLICATE, SESSION_FULL)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping("/{sessionId}/enrollments")
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentResponse enroll(
      @PathVariable Long sessionId,
      @Parameter(description = "신청자 임직원 ID (임시, NFR-03 전까지)", required = true)
          @RequestHeader(EMPLOYEE_ID_HEADER)
          Long employeeId) {
    return enrollmentCommandService.enroll(sessionId, employeeId);
  }
}
