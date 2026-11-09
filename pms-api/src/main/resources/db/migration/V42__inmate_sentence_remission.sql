ALTER TABLE inmates
    ADD COLUMN sentence_type VARCHAR(20) NOT NULL DEFAULT 'FIXED_TERM',
    ADD COLUMN remission_percentage NUMERIC(5, 2),
    ADD COLUMN projected_release_date DATE;

UPDATE inmates SET projected_release_date = sentence_end_date;

ALTER TABLE inmates
    ALTER COLUMN sentence_duration_value DROP NOT NULL,
    ALTER COLUMN sentence_duration_unit DROP NOT NULL,
    ALTER COLUMN sentence_end_date DROP NOT NULL,
    ADD CONSTRAINT ck_inmates_sentence_type CHECK (sentence_type IN ('FIXED_TERM', 'LIFE', 'UNDETERMINED')),
    ADD CONSTRAINT ck_inmates_remission_percentage CHECK (remission_percentage IS NULL OR remission_percentage BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_inmates_sentence_details CHECK (
        (sentence_type = 'FIXED_TERM' AND sentence_end_date IS NOT NULL
            AND sentence_end_date >= sentence_start_date AND sentence_duration_value IS NOT NULL
            AND sentence_duration_unit IS NOT NULL)
        OR (sentence_type IN ('LIFE', 'UNDETERMINED') AND sentence_end_date IS NULL
            AND sentence_duration_value IS NULL AND sentence_duration_unit IS NULL
            AND remission_percentage IS NULL AND projected_release_date IS NULL)
    );
