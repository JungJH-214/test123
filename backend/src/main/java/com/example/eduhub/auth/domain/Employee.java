package com.example.eduhub.auth.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 임직원 계정. 비밀번호는 해시로만 저장한다. */
@Entity
@Table(name = "employee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "employee_no", nullable = false, length = 20)
  private String employeeNo;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  private Employee(String employeeNo, String name, String passwordHash, Role role) {
    this.employeeNo = employeeNo;
    this.name = name;
    this.passwordHash = passwordHash;
    this.role = role;
  }

  public static Employee create(String employeeNo, String name, String passwordHash, Role role) {
    return new Employee(employeeNo, name, passwordHash, role);
  }
}
