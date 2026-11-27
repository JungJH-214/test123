package com.example.eduhub.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 수료 처리 요청 (FR-08). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompletionRequest {

  @Schema(description = "수료 처리할 임직원 ID 목록. 중복은 한 번만 처리한다", example = "[1, 2, 3]")
  @NotEmpty(message = "수료 처리할 임직원을 하나 이상 지정해야 합니다")
  private List<@NotNull(message = "임직원 ID는 비울 수 없습니다") Long> employeeIds;

  public CompletionRequest(List<Long> employeeIds) {
    this.employeeIds = employeeIds;
  }
}
