package com.example.eduhub.auth.dto;

/** 로그인 응답 (NFR-03). */
public final class LoginResponse {

  private final String accessToken;
  private final String tokenType;
  private final long expiresIn;
  private final UserResponse user;

  private LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
    this.accessToken = accessToken;
    this.tokenType = tokenType;
    this.expiresIn = expiresIn;
    this.user = user;
  }

  public static LoginResponse of(String accessToken, long expiresIn, UserResponse user) {
    return new LoginResponse(accessToken, "Bearer", expiresIn, user);
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public UserResponse getUser() {
    return user;
  }
}
