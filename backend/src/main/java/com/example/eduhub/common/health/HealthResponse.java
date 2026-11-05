package com.example.eduhub.common.health;

/** 헬스 체크 응답. */
public final class HealthResponse {

  private final String status;

  private HealthResponse(String status) {
    this.status = status;
  }

  public static HealthResponse up() {
    return new HealthResponse("UP");
  }

  public String getStatus() {
    return status;
  }
}
