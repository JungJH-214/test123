package com.example.eduhub.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;
import com.example.eduhub.auth.dto.LoginRequest;
import com.example.eduhub.auth.dto.LoginResponse;
import com.example.eduhub.auth.dto.UserResponse;
import com.example.eduhub.auth.exception.InvalidCredentialsException;
import com.example.eduhub.auth.repository.EmployeeRepository;
import com.example.eduhub.common.error.UnauthorizedException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private EmployeeRepository employeeRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtIssuer jwtIssuer;

  private AuthService authService;
  private Employee employee;

  @BeforeEach
  void setUp() {
    given(passwordEncoder.encode("dummy-password-for-timing")).willReturn("DUMMY");
    authService = new AuthService(employeeRepository, passwordEncoder, jwtIssuer);
    employee = Employee.create("E0001", "사원1", "HASH", Role.EMPLOYEE);
    ReflectionTestUtils.setField(employee, "id", 7L);
  }

  @Test
  void login_success_returnsTokenAndUser() {
    given(employeeRepository.findByEmployeeNo("E0001")).willReturn(Optional.of(employee));
    given(passwordEncoder.matches("pw", "HASH")).willReturn(true);
    given(jwtIssuer.issue(employee)).willReturn(new JwtIssuer.IssuedToken("tok", 3600L));

    LoginResponse response = authService.login(new LoginRequest("E0001", "pw"));

    assertThat(response.getAccessToken()).isEqualTo("tok");
    assertThat(response.getTokenType()).isEqualTo("Bearer");
    assertThat(response.getExpiresIn()).isEqualTo(3600L);
    assertThat(response.getUser().getEmployeeNo()).isEqualTo("E0001");
    assertThat(response.getUser().getRole()).isEqualTo(Role.EMPLOYEE);
  }

  @Test
  void login_wrongPassword_throwsInvalidCredentials() {
    given(employeeRepository.findByEmployeeNo("E0001")).willReturn(Optional.of(employee));
    given(passwordEncoder.matches("bad", "HASH")).willReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest("E0001", "bad")))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("사번 또는 비밀번호가 올바르지 않습니다.");
    verify(jwtIssuer, never()).issue(any());
  }

  @Test
  void login_unknownEmployeeNo_throwsSameException_andChecksDummyHash() {
    given(employeeRepository.findByEmployeeNo("X")).willReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("X", "pw")))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("사번 또는 비밀번호가 올바르지 않습니다.");
    verify(passwordEncoder).matches(eq("pw"), eq("DUMMY"));
    verify(jwtIssuer, never()).issue(any());
  }

  @Test
  void me_returnsUser() {
    given(employeeRepository.findById(7L)).willReturn(Optional.of(employee));

    UserResponse response = authService.me(7L);

    assertThat(response.getId()).isEqualTo(7L);
    assertThat(response.getEmployeeNo()).isEqualTo("E0001");
    assertThat(response.getName()).isEqualTo("사원1");
  }

  @Test
  void me_unknown_throwsUnauthorized() {
    given(employeeRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> authService.me(99L))
        .isInstanceOf(UnauthorizedException.class)
        .extracting("code")
        .isEqualTo("UNAUTHORIZED");
  }
}
