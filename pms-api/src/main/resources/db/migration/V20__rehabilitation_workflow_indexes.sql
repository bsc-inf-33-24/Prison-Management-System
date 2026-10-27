-- Preserve the existing rehabilitation rows and identifiers while supporting
-- program, inmate, and enrollment scoped workflow lookups.
CREATE INDEX IF NOT EXISTS idx_enrollment_program_status_date
    ON enrollment (program_id, status, enrollment_date DESC, enrollment_id DESC);

CREATE INDEX IF NOT EXISTS idx_enrollment_inmate_date
    ON enrollment (inmate_id, enrollment_date DESC, enrollment_id DESC);

CREATE INDEX IF NOT EXISTS idx_attendance_enrollment_date
    ON attendancerecord (enrollment_id, session_date DESC, attendance_id DESC);
