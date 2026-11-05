/**
 * 교육과정 도메인: 과정과 회차의 등록·수정·종료, 목록·상세 조회 (FR-01~03).
 *
 * <p>계층 규칙: Controller → Service → Repository. Controller는 Repository를 직접 호출하지 않고, API 응답은 엔티티가 아닌
 * DTO로 반환한다.
 *
 * <p>course는 enrollment에 의존하지 않는다.
 */
package com.example.eduhub.course;
