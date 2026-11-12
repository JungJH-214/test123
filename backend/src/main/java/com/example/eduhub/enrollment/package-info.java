/**
 * 수강 신청 도메인: 신청, 대기, 취소, 대기 승급, 수료 (FR-04~09, D-01~03).
 *
 * <p>계층 규칙: Controller → Service → Repository. Controller는 Repository를 직접 호출하지 않고, API 응답은 엔티티가 아닌
 * DTO로 반환한다.
 *
 * <p>의존 방향은 enrollment → course만 허용한다.
 */
package com.example.eduhub.enrollment;
