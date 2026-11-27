package com.example.eduhub.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** 수료 처리에 실패한 한 건 (FR-08). */
@Getter
public final class CompletionFailure {

  @Schema(description = "임직원 ID")
  private final Long employeeId;

  @Schema(description = "실패 사유")
  private final CompletionFailureReason reason;

  public CompletionFailure(Long employeeId, CompletionFailureReason reason) {
    this.employeeId = employeeId;
    this.reason = reason;
  }
}
