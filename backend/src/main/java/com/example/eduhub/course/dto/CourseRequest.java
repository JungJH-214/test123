package com.example.eduhub.course.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 과정 등록·수정 요청 (FR-03). */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public final class CourseRequest {

  @NotBlank(message = "필수 입력입니다")
  @Size(max = 200, message = "200자 이하로 입력하세요")
  private String title;

  @NotBlank(message = "필수 입력입니다")
  @Size(max = 50, message = "50자 이하로 입력하세요")
  private String category;

  @Size(max = 2000, message = "2000자 이하로 입력하세요")
  private String description;
}
