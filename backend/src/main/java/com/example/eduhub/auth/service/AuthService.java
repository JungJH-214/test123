package com.example.eduhub.auth.service;

import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.dto.LoginRequest;
import com.example.eduhub.auth.dto.LoginResponse;
import com.example.eduhub.auth.dto.UserResponse;
import com.example.eduhub.auth.exception.InvalidCredentialsException;
import com.example.eduhub.auth.repository.EmployeeRepository;
import com.example.eduhub.common.error.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인과 내 정보 조회 (NFR-03).
 *
 * <p>사번이 없거나 비밀번호가 틀리면 같은 INVALID_CREDENTIALS로 응답한다. 사번이 없을 때도 더미 해시로 비밀번호 비교를 한 번 실행해 응답 시간 차이로 사번
 * 존재 여부가 드러나지 않게 한다. 토큰은 유효하지만 사원이 없으면 me는 401 UNAUTHORIZED다.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

  private final EmployeeRepository employeeRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtIssuer jwtIssuer;
  private final String dummyHash;

  public AuthService(
      EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder, JwtIssuer jwtIssuer) {
    this.employeeRepository = employeeRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtIssuer = jwtIssuer;
    this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
  }

  public LoginResponse login(LoginRequest request) {
    Employee employee = employeeRepository.findByEmployeeNo(request.getEmployeeNo()).orElse(null);
    if (employee == null) {
      passwordEncoder.matches(request.getPassword(), dummyHash);
      throw new InvalidCredentialsException();
    }
    if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    JwtIssuer.IssuedToken issued = jwtIssuer.issue(employee);
    return LoginResponse.of(
        issued.getToken(), issued.getExpiresInSeconds(), UserResponse.from(employee));
  }

  public UserResponse me(Long employeeId) {
    return employeeRepository
        .findById(employeeId)
        .map(UserResponse::from)
        .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "사용자를 찾을 수 없습니다."));
  }
}
