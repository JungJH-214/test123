package com.example.eduhub.auth.dto;

import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;

/** 사용자 정보. 비밀번호 해시는 절대 넣지 않는다. */
public final class UserResponse {

  private final Long id;
  private final String employeeNo;
  private final String name;
  private final Role role;

  private UserResponse(Long id, String employeeNo, String name, Role role) {
    this.id = id;
    this.employeeNo = employeeNo;
    this.name = name;
    this.role = role;
  }

  public static UserResponse from(Employee employee) {
    return new UserResponse(
        employee.getId(), employee.getEmployeeNo(), employee.getName(), employee.getRole());
  }

  public Long getId() {
    return id;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public String getName() {
    return name;
  }

  public Role getRole() {
    return role;
  }
}
