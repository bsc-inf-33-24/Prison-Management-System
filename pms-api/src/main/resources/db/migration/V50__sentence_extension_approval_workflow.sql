CREATE TABLE sentence_extension_requests (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    sentence_id BIGINT NOT NULL REFERENCES inmate_sentences(id) ON DELETE RESTRICT,
    original_end_date DATE NOT NULL,
    proposed_end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    requester_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    reviewer_user_id BIGINT REFERENCES users(user_id) ON DELETE RESTRICT,
    decision VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sentence_extension_dates CHECK (proposed_end_date > original_end_date),
    CONSTRAINT ck_sentence_extension_decision CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_sentence_extension_review CHECK (
        (decision = 'PENDING' AND reviewer_user_id IS NULL AND decided_at IS NULL)
        OR (decision IN ('APPROVED', 'REJECTED') AND reviewer_user_id IS NOT NULL AND decided_at IS NOT NULL)
    )
);

CREATE INDEX idx_sentence_extension_requests_sentence
    ON sentence_extension_requests(sentence_id, requested_at DESC);
CREATE UNIQUE INDEX uk_sentence_extension_requests_pending
    ON sentence_extension_requests(sentence_id) WHERE decision = 'PENDING';
