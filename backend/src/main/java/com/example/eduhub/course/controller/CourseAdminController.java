package com.example.eduhub.course.controller;

import com.example.eduhub.common.error.ErrorResponse;
import com.example.eduhub.course.dto.CourseDetailResponse;
import com.example.eduhub.course.dto.CourseRequest;
import com.example.eduhub.course.dto.SessionRequest;
import com.example.eduhub.course.dto.SessionResponse;
import com.example.eduhub.course.service.CourseCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 과정·회차 등록, 수정, 종료 (FR-03).
 *
 * <p>TODO(NFR-03): 인증이 아직 없어 권한 검사를 하지 않는다. 인증을 넣을 때 /api/v1/admin/** 에 관리자 권한을 건다.
 */
@Tag(name = "CourseAdmin", description = "교육과정 관리 (관리자)")
@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
public class CourseAdminController {

  private final CourseCommandService courseCommandService;

  @Operation(summary = "과정 등록")
  @ApiResponse(responseCode = "201", description = "등록 성공")
  @ApiResponse(
      responseCode = "400",
      description = "입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CourseDetailResponse createCourse(@Valid @RequestBody CourseRequest request) {
    return courseCommandService.createCourse(request);
  }

  @Operation(summary = "과정 수정 (종료된 과정은 409)")
  @ApiResponse(responseCode = "200", description = "수정 성공")
  @ApiResponse(
      responseCode = "400",
      description = "입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "과정이 없음 (COURSE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "종료된 과정 (COURSE_CLOSED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{courseId}")
  public CourseDetailResponse updateCourse(
      @PathVariable Long courseId, @Valid @RequestBody CourseRequest request) {
    return courseCommandService.updateCourse(courseId, request);
  }

  @Operation(summary = "과정 종료 (회차도 모두 종료, 이미 종료돼 있으면 그대로 반환)")
  @ApiResponse(responseCode = "200", description = "종료 성공")
  @ApiResponse(
      responseCode = "404",
      description = "과정이 없음 (COURSE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping("/{courseId}/close")
  public CourseDetailResponse closeCourse(@PathVariable Long courseId) {
    return courseCommandService.closeCourse(courseId);
  }

  @Operation(summary = "회차 등록 (종료된 과정은 409)")
  @ApiResponse(responseCode = "201", description = "등록 성공")
  @ApiResponse(
      responseCode = "400",
      description = "입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "과정이 없음 (COURSE_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "종료된 과정 (COURSE_CLOSED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping("/{courseId}/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  public SessionResponse createSession(
      @PathVariable Long courseId, @Valid @RequestBody SessionRequest request) {
    return courseCommandService.createSession(courseId, request);
  }

  @Operation(summary = "회차 수정 (종료된 회차는 409)")
  @ApiResponse(responseCode = "200", description = "수정 성공")
  @ApiResponse(
      responseCode = "400",
      description = "입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "과정 또는 회차가 없음 (COURSE_NOT_FOUND, SESSION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "종료된 회차 (SESSION_CLOSED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping("/{courseId}/sessions/{sessionId}")
  public SessionResponse updateSession(
      @PathVariable Long courseId,
      @PathVariable Long sessionId,
      @Valid @RequestBody SessionRequest request) {
    return courseCommandService.updateSession(courseId, sessionId, request);
  }

  @Operation(summary = "회차 종료 (이미 종료돼 있으면 그대로 반환)")
  @ApiResponse(responseCode = "200", description = "종료 성공")
  @ApiResponse(
      responseCode = "404",
      description = "과정 또는 회차가 없음 (COURSE_NOT_FOUND, SESSION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping("/{courseId}/sessions/{sessionId}/close")
  public SessionResponse closeSession(@PathVariable Long courseId, @PathVariable Long sessionId) {
    return courseCommandService.closeSession(courseId, sessionId);
  }
}
