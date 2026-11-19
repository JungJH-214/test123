-- (회차, 임직원) 중복 신청 방지. 취소된 신청은 제외 (FR-04)
CREATE UNIQUE INDEX ux_enrollment_session_employee_active
    ON enrollment (session_id, employee_id)
    WHERE status <> 'CANCELLED';
