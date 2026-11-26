package com.example.eduhub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;
import com.example.eduhub.auth.repository.EmployeeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DevAccountInitializerTest {

  @Mock private EmployeeRepository employeeRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @Test
  void run_noPassword_createsNothing() {
    DevAccountInitializer initializer =
        new DevAccountInitializer(employeeRepository, passwordEncoder, "");

    initializer.run(null);

    verifyNoInteractions(employeeRepository, passwordEncoder);
  }

  @Test
  void run_createsThreeAccounts() {
    given(employeeRepository.existsByEmployeeNo(any())).willReturn(false);
    given(passwordEncoder.encode("test-dev-pw")).willReturn("HASH");
    DevAccountInitializer initializer =
        new DevAccountInitializer(employeeRepository, passwordEncoder, "test-dev-pw");

    initializer.run(null);

    ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
    verify(employeeRepository, times(3)).save(captor.capture());
    List<Employee> saved = captor.getAllValues();
    assertThat(saved)
        .extracting(Employee::getEmployeeNo)
        .containsExactly("E0001", "E0002", "A0001");
    assertThat(saved).filteredOn(e -> e.getRole() == Role.ADMIN).hasSize(1);
    assertThat(saved).filteredOn(e -> e.getRole() == Role.EMPLOYEE).hasSize(2);
    assertThat(saved).extracting(Employee::getPasswordHash).containsOnly("HASH");
  }

  @Test
  void run_duplicateOnSave_continues() {
    given(employeeRepository.existsByEmployeeNo(any())).willReturn(false);
    given(passwordEncoder.encode("test-dev-pw")).willReturn("HASH");
    given(employeeRepository.save(any(Employee.class)))
        .willThrow(new DataIntegrityViolationException("dup"))
        .willAnswer(inv -> inv.getArgument(0));
    DevAccountInitializer initializer =
        new DevAccountInitializer(employeeRepository, passwordEncoder, "test-dev-pw");

    initializer.run(null);

    verify(employeeRepository, times(3)).save(any(Employee.class));
  }

  @Test
  void run_existingAccount_skipped() {
    given(employeeRepository.existsByEmployeeNo("E0001")).willReturn(true);
    given(employeeRepository.existsByEmployeeNo("E0002")).willReturn(false);
    given(employeeRepository.existsByEmployeeNo("A0001")).willReturn(false);
    given(passwordEncoder.encode("test-dev-pw")).willReturn("HASH");
    DevAccountInitializer initializer =
        new DevAccountInitializer(employeeRepository, passwordEncoder, "test-dev-pw");

    initializer.run(null);

    ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
    verify(employeeRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(Employee::getEmployeeNo)
        .containsExactly("E0002", "A0001");
  }
}
