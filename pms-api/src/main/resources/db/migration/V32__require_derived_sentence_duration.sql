UPDATE inmates
SET sentence_start_date = COALESCE(sentence_start_date, admission_date),
    sentence_end_date = CASE
        WHEN sentence_end_date IS NULL OR sentence_end_date < COALESCE(sentence_start_date, admission_date)
            THEN COALESCE(sentence_start_date, admission_date)
        ELSE sentence_end_date
    END;

UPDATE inmates
SET sentence_duration_value = CASE
        WHEN sentence_end_date + 1 = sentence_start_date +
             (EXTRACT(YEAR FROM age(sentence_end_date + 1, sentence_start_date))::INTEGER * INTERVAL '1 year')
             AND EXTRACT(YEAR FROM age(sentence_end_date + 1, sentence_start_date)) > 0
            THEN EXTRACT(YEAR FROM age(sentence_end_date + 1, sentence_start_date))::INTEGER
        WHEN EXTRACT(DAY FROM age(sentence_end_date + 1, sentence_start_date)) = 0
             AND (EXTRACT(YEAR FROM age(sentence_end_date + 1, sentence_start_date)) * 12
                  + EXTRACT(MONTH FROM age(sentence_end_date + 1, sentence_start_date))) > 0
            THEN (EXTRACT(YEAR FROM age(sentence_end_date + 1, sentence_start_date)) * 12
                  + EXTRACT(MONTH FROM age(sentence_end_date + 1, sentence_start_date)))::INTEGER
        ELSE (sentence_end_date - sentence_start_date + 1)
    END,
    sentence_duration_unit = CASE
        WHEN sentence_end_date + 1 = sentence_start_date +
             (EXTRACT(YEAR FROM age(sentence_end_date + 1, sentence_start_date))::INTEGER * INTERVAL '1 year')
             AND EXTRACT(YEAR FROM age(sentence_end_date + 1, sentence_start_date)) > 0
            THEN 'YEARS'
        WHEN EXTRACT(DAY FROM age(sentence_end_date + 1, sentence_start_date)) = 0
             AND (EXTRACT(YEAR FROM age(sentence_end_date + 1, sentence_start_date)) * 12
                  + EXTRACT(MONTH FROM age(sentence_end_date + 1, sentence_start_date))) > 0
            THEN 'MONTHS'
        ELSE 'DAYS'
    END;

ALTER TABLE inmates
    ALTER COLUMN sentence_duration_value SET NOT NULL,
    ALTER COLUMN sentence_duration_unit SET NOT NULL,
    ALTER COLUMN sentence_start_date SET NOT NULL,
    ALTER COLUMN sentence_end_date SET NOT NULL,
    ADD CONSTRAINT ck_inmates_sentence_dates_ordered
        CHECK (sentence_end_date >= sentence_start_date);
