ALTER TABLE inmates
    ADD COLUMN home_of_origin VARCHAR(255),
    DROP COLUMN contact_phone,
    DROP COLUMN contact_email;