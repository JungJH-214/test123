package com.example.eduhub.enrollment.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 수료 처리 요청 (FR-08). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompletionRequest {

  @ArraySchema(
      maxItems = 500,
      schema = @Schema(description = "임직원 ID", example = "1"),
      arraySchema = @Schema(description = "수료 처리할 임직원 ID 목록(최대 500명). 중복은 한 번만 처리한다"))
  @NotEmpty(message = "수료 처리할 임직원을 하나 이상 지정해야 합니다")
  @Size(max = 500, message = "한 번에 500명까지 처리할 수 있습니다")
  private List<@NotNull(message = "임직원 ID는 비울 수 없습니다") Long> employeeIds;

  public CompletionRequest(List<Long> employeeIds) {
    this.employeeIds = employeeIds;
  }
}
