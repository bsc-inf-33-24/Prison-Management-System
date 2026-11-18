INSERT INTO custody_stays (public_uuid, inmate_id, prison_id, admission_date, ended_at, created_at, updated_at)
SELECT gen_random_uuid(), i.id, i.current_prison_id, i.admission_date, NULL, NOW(), NOW()
FROM inmates i
JOIN cell_allocations a ON a.inmate_id = i.id AND a.released_at IS NULL
WHERE i.current_prison_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM custody_stays s WHERE s.inmate_id = i.id AND s.ended_at IS NULL
  );
