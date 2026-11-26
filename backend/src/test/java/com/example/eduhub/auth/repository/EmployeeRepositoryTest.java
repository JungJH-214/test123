package com.example.eduhub.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("pgtest")
class EmployeeRepositoryTest {

  @Autowired private EmployeeRepository employeeRepository;
  @Autowired private TestEntityManager em;

  @Test
  void findByEmployeeNo_returnsEmployee() {
    Employee saved = employeeRepository.save(Employee.create("T0001", "홍길동", "hash", Role.ADMIN));
    em.flush();
    em.clear();

    Employee found = employeeRepository.findByEmployeeNo("T0001").orElseThrow();
    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getName()).isEqualTo("홍길동");
    assertThat(found.getPasswordHash()).isEqualTo("hash");
    assertThat(found.getRole()).isEqualTo(Role.ADMIN);
  }

  @Test
  void findByEmployeeNo_unknown_returnsEmpty() {
    assertThat(employeeRepository.findByEmployeeNo("NONE")).isEmpty();
  }

  @Test
  void existsByEmployeeNo_trueAndFalse() {
    employeeRepository.save(Employee.create("T0002", "사원", "hash", Role.EMPLOYEE));
    em.flush();
    em.clear();

    assertThat(employeeRepository.existsByEmployeeNo("T0002")).isTrue();
    assertThat(employeeRepository.existsByEmployeeNo("T9999")).isFalse();
  }

  @Test
  void save_duplicateEmployeeNo_throwsDataIntegrityViolation() {
    employeeRepository.saveAndFlush(Employee.create("T0003", "사원", "hash", Role.EMPLOYEE));

    assertThatThrownBy(
            () ->
                employeeRepository.saveAndFlush(
                    Employee.create("T0003", "다른사원", "hash2", Role.EMPLOYEE)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
