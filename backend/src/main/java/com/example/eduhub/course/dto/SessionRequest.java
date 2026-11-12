package com.example.eduhub.course.dto;

import java.time.LocalDate;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회차 등록·수정 요청 (FR-03). 지난 날짜도 허용한다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public final class SessionRequest {

  @NotNull(message = "필수 입력입니다")
  private LocalDate startDate;

  @NotBlank(message = "필수 입력입니다")
  @Size(max = 200, message = "200자 이하로 입력하세요")
  private String place;

  @NotNull(message = "필수 입력입니다")
  @Min(value = 1, message = "1 이상이어야 합니다")
  private Integer capacity;
}
