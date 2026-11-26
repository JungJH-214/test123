package com.example.eduhub.auth.controller;

import com.example.eduhub.auth.CurrentUser;
import com.example.eduhub.auth.dto.UserResponse;
import com.example.eduhub.auth.service.AuthService;
import com.example.eduhub.common.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 현재 사용자 조회 (NFR-03). */
@Tag(name = "Auth", description = "인증")
@RestController
@RequiredArgsConstructor
public class MeController {

  private final AuthService authService;

  @Operation(summary = "내 정보")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  @ApiResponse(
      responseCode = "401",
      description = "토큰 없음·무효, 사원 없음 (UNAUTHORIZED)",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @GetMapping("/api/me")
  public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return authService.me(CurrentUser.from(jwt).getEmployeeId());
  }
}
