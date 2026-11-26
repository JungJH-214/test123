package com.example.eduhub.auth;

import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;
import com.example.eduhub.auth.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * dev 프로필에서 개발용 계정 3개를 만든다 (NFR-03). 비밀번호는 EDUHUB_DEV_PASSWORD 하나를 같이 쓰고, 없으면 만들지 않는다. 이미 있는 사번은
 * 건너뛴다. 계정마다 단건 저장이라 서로 독립이며, 겹치는 경우는 DB 유일 제약이 막는다.
 */
@Slf4j
@Component
@Profile("dev")
public class DevAccountInitializer implements ApplicationRunner {

  private final EmployeeRepository employeeRepository;
  private final PasswordEncoder passwordEncoder;
  private final String devPassword;

  public DevAccountInitializer(
      EmployeeRepository employeeRepository,
      PasswordEncoder passwordEncoder,
      @Value("${eduhub.dev-password:}") String devPassword) {
    this.employeeRepository = employeeRepository;
    this.passwordEncoder = passwordEncoder;
    this.devPassword = devPassword;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (devPassword == null || devPassword.isEmpty()) {
      log.warn("EDUHUB_DEV_PASSWORD가 없어 개발용 계정을 만들지 않습니다");
      return;
    }
    createIfAbsent("E0001", "사원1", Role.EMPLOYEE);
    createIfAbsent("E0002", "사원2", Role.EMPLOYEE);
    createIfAbsent("A0001", "관리자", Role.ADMIN);
  }

  private void createIfAbsent(String employeeNo, String name, Role role) {
    if (employeeRepository.existsByEmployeeNo(employeeNo)) {
      return;
    }
    try {
      employeeRepository.save(
          Employee.create(employeeNo, name, passwordEncoder.encode(devPassword), role));
      log.info("개발용 계정을 만들었습니다: {}", employeeNo);
    } catch (DataIntegrityViolationException e) {
      log.info("다른 서버가 먼저 만든 개발용 계정입니다: {}", employeeNo);
    }
  }
}
