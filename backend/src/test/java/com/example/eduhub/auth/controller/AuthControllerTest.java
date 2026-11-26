package com.example.eduhub.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eduhub.auth.config.JsonSecurityErrorHandler;
import com.example.eduhub.auth.config.SecurityConfig;
import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;
import com.example.eduhub.auth.dto.LoginResponse;
import com.example.eduhub.auth.dto.UserResponse;
import com.example.eduhub.auth.exception.InvalidCredentialsException;
import com.example.eduhub.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@Import({SecurityConfig.class, JsonSecurityErrorHandler.class})
@WebMvcTest({AuthController.class, MeController.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private AuthService authService;

  private UserResponse user() {
    Employee employee = Employee.create("E0001", "사원1", "HASH", Role.EMPLOYEE);
    ReflectionTestUtils.setField(employee, "id", 7L);
    return UserResponse.from(employee);
  }

  @Test
  void login_returns200() throws Exception {
    given(authService.login(any())).willReturn(LoginResponse.of("tok", 3600L, user()));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeNo\":\"E0001\",\"password\":\"pw\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("tok"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600))
        .andExpect(jsonPath("$.user.employeeNo").value("E0001"))
        .andExpect(jsonPath("$.user.role").value("EMPLOYEE"))
        .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
  }

  @Test
  void login_invalid_returns401() throws Exception {
    given(authService.login(any())).willThrow(new InvalidCredentialsException());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeNo\":\"E0001\",\"password\":\"bad\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void login_blank_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeNo\":\"\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    verifyNoInteractions(authService);
  }

  @Test
  void me_noToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    verifyNoInteractions(authService);
  }

  @Test
  void me_withJwt_returns200() throws Exception {
    given(authService.me(7L)).willReturn(user());

    mockMvc
        .perform(get("/api/me").with(jwt().jwt(j -> j.subject("7").claim("role", "EMPLOYEE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.employeeNo").value("E0001"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    verify(authService).me(7L);
  }
}
