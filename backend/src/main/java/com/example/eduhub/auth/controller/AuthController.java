package com.example.eduhub.auth.controller;

import com.example.eduhub.auth.dto.LoginRequest;
import com.example.eduhub.auth.dto.LoginResponse;
import com.example.eduhub.auth.service.AuthService;
import com.example.eduhub.common.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 (NFR-03). 공개 API. */
@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "로그인 (JWT 발급)")
  @SecurityRequirements()
  @ApiResponse(responseCode = "200", description = "로그인 성공")
  @ApiResponse(
      responseCode = "400",
      description = "입력값 오류 (VALIDATION_FAILED, INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "401",
      description = "사번 또는 비밀번호 오류 (INVALID_CREDENTIALS)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
