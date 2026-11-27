package com.example.eduhub.common.time;

import java.time.ZoneId;

/** 업무 날짜를 정하는 시간대. 서버 Clock은 UTC라서 날짜 비교는 이 시간대로 바꿔서 한다. */
public final class BusinessTime {

  public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private BusinessTime() {}
}
