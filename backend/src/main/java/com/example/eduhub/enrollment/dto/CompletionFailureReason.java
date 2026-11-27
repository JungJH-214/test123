package com.example.eduhub.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 수료 처리에 실패한 사유 (FR-08). 같은 임직원의 신청이 여러 건이면 ID가 큰(최신) 신청의 상태로 판단하며, 취소된 신청이 최신이면 CANCELLED다. */
@Schema(description = "수료 처리 실패 사유")
public enum CompletionFailureReason {
  @Schema(description = "이 회차에 신청 내역이 없음")
  NOT_ENROLLED,
  @Schema(description = "대기 상태라 수료 처리할 수 없음")
  WAITING,
  @Schema(description = "이미 수료 처리됨")
  ALREADY_COMPLETED,
  @Schema(description = "취소된 신청이라 수료 처리할 수 없음 (최신 신청이 취소 상태)")
  CANCELLED
}
