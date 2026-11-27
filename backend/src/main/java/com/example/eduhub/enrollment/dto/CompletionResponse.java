package com.example.eduhub.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

/** 수료 처리 결과 (FR-08). 한 건이 실패해도 나머지는 처리된다. */
@Getter
public final class CompletionResponse {

  @Schema(description = "수료 처리된 임직원 ID. 요청 순서")
  private final List<Long> completed;

  @Schema(description = "수료 처리하지 못한 임직원과 사유. 요청 순서")
  private final List<CompletionFailure> failed;

  public CompletionResponse(List<Long> completed, List<CompletionFailure> failed) {
    this.completed = completed;
    this.failed = failed;
  }
}
