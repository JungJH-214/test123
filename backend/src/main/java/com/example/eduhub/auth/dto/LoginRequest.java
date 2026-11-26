package com.example.eduhub.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그인 요청 (NFR-03). */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public final class LoginRequest {

  @NotBlank(message = "필수 입력입니다")
  @Size(max = 20, message = "20자 이하로 입력하세요")
  private String employeeNo;

  @NotBlank(message = "필수 입력입니다")
  @Size(max = 100, message = "100자 이하로 입력하세요")
  private String password;
}
