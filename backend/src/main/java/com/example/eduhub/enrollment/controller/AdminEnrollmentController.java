package com.example.eduhub.enrollment.controller;

import com.example.eduhub.common.error.ErrorResponse;
import com.example.eduhub.enrollment.dto.CompletionRequest;
import com.example.eduhub.enrollment.dto.CompletionResponse;
import com.example.eduhub.enrollment.dto.RosterResponse;
import com.example.eduhub.enrollment.service.RosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 수강 관리: 회차 명단과 수료 처리 (FR-08). ADMIN만 접근한다 (D-03, NFR-03). */
@Tag(name = "Roster", description = "회차 명단 조회와 수료 처리 (관리자)")
@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
public class AdminEnrollmentController {

  private final RosterService rosterService;

  @Operation(summary = "회차 명단 조회 (확정자·대기자)")
  @ApiResponse(responseCode = "200", description = "명단 (확정자, 대기자와 대기 순번)")
  @ApiResponse(
      responseCode = "401",
      description = "토큰이 없거나 올바르지 않음 (UNAUTHORIZED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "관리자가 아님 (FORBIDDEN)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "회차가 없음 (SESSION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/{sessionId}/roster")
  public RosterResponse getRoster(@PathVariable Long sessionId) {
    return rosterService.getRoster(sessionId);
  }

  @Operation(
      summary = "수료 처리",
      description = "확정자만 수료 처리한다. 일부가 실패해도 나머지는 처리하고 실패 사유를 돌려준다. 회차 시작일(KST) 전에는 할 수 없다.")
  @ApiResponse(responseCode = "200", description = "처리 결과 (수료된 ID와 실패 사유)")
  @ApiResponse(
      responseCode = "400",
      description = "임직원 ID 목록이 비었거나 형식이 올바르지 않음 (VALIDATION_FAILED, INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "토큰이 없거나 올바르지 않음 (UNAUTHORIZED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "관리자가 아님 (FORBIDDEN)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "회차가 없음 (SESSION_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "회차가 아직 시작되지 않음 (SESSION_NOT_STARTED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping("/{sessionId}/completions")
  public CompletionResponse complete(
      @PathVariable Long sessionId, @Valid @RequestBody CompletionRequest request) {
    return rosterService.complete(sessionId, request.getEmployeeIds());
  }
}
