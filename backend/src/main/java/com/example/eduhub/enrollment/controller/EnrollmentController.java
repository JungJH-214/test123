package com.example.eduhub.enrollment.controller;

import com.example.eduhub.auth.CurrentUser;
import com.example.eduhub.common.error.ErrorResponse;
import com.example.eduhub.enrollment.dto.EnrollmentResponse;
import com.example.eduhub.enrollment.service.EnrollmentCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회차 수강 신청 (FR-04, NFR-01).
 *
 * <p>신청자는 토큰의 사원(NFR-03)이다. 요청의 헤더나 본문 값은 신청자로 쓰지 않는다.
 */
@Tag(name = "Enrollment", description = "수강 신청")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class EnrollmentController {

  private final EnrollmentCommandService enrollmentCommandService;

  @Operation(summary = "수강 신청 (정원 안이면 확정, 정원이 차면 대기)")
  @ApiResponse(responseCode = "201", description = "신청 성공 (status: CONFIRMED 또는 WAITING)")
  @ApiResponse(
      responseCode = "400",
      description = "경로 값 형식 오류 (INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "토큰이 없거나 올바르지 않음 (UNAUTHORIZED)",
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
  public EnrollmentResponse enroll(@PathVariable Long sessionId, @AuthenticationPrincipal Jwt jwt) {
    return enrollmentCommandService.enroll(sessionId, CurrentUser.from(jwt).getEmployeeId());
  }
}
