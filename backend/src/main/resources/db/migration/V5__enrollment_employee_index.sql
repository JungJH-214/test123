-- 내 신청 내역 조회 (FR-07)
CREATE INDEX idx_enrollment_employee_created ON enrollment (employee_id, created_at);
