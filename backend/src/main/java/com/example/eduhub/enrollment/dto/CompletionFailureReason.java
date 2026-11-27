package com.example.eduhub.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 수료 처리에 실패한 사유 (FR-08). FR-06이 CANCELLED를 추가하면 CANCELLED도 넣는다. */
@Schema(description = "수료 처리 실패 사유")
public enum CompletionFailureReason {
  @Schema(description = "이 회차에 신청 내역이 없음")
  NOT_ENROLLED,
  @Schema(description = "대기 상태라 수료 처리할 수 없음")
  WAITING,
  @Schema(description = "이미 수료 처리됨")
  ALREADY_COMPLETED
}
