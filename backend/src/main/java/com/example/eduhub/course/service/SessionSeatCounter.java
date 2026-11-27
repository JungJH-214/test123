package com.example.eduhub.course.service;

import java.util.Collection;
import java.util.Map;

/** 회차별 정원을 차지한 수를 알려 주는 포트. course는 enrollment에 의존하지 않으므로 인터페이스만 두고 구현은 enrollment가 한다. */
public interface SessionSeatCounter {

  /** 회차 ID별로 정원을 차지한 수(확정 + 수료, D-05)를 돌려준다. 신청이 없는 회차는 맵에 없을 수 있으므로 호출 쪽이 0으로 본다. 빈 입력이면 빈 맵이다. */
  Map<Long, Long> countSeatsTaken(Collection<Long> sessionIds);
}
