ALTER TABLE inmates
    ADD COLUMN offence VARCHAR(500),
    ADD COLUMN sentence_duration_value INTEGER,
    ADD COLUMN sentence_duration_unit VARCHAR(20),
    ADD COLUMN sentence_start_date DATE,
    ADD COLUMN sentence_end_date DATE;
    ADD CONSTRAINT ck_inmates_sentence_duration_positive
        CHECK (sentence_duration_value IS NULL OR sentence_duration_value > 0),
    ADD CONSTRAINT ck_inmates_sentence_duration_unit
        CHECK (sentence_duration_unit IS NULL OR sentence_duration_unit IN ('DAYS', 'MONTHS', 'YEARS')),
    ADD CONSTRAINT ck_inmates_sentence_duration_pair
        CHECK ((sentence_duration_value IS NULL) = (sentence_duration_unit IS NULL));
