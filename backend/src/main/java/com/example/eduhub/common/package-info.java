/**
 * 여러 도메인이 함께 쓰는 공통 기능.
 *
 * <ul>
 *   <li>{@code config}: OpenAPI 등 애플리케이션 설정
 *   <li>{@code health}: 서버 상태 확인 API
 * </ul>
 *
 * <p>common은 다른 도메인 패키지(course, enrollment)에 의존하지 않는다.
 */
package com.example.eduhub.common;
