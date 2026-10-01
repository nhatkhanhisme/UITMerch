CREATE TABLE background_jobs (
    id UUID PRIMARY KEY,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('EMAIL', 'EMBEDDING')),
    payload TEXT NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (state IN ('PENDING', 'PROCESSING', 'DONE', 'DEAD')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_background_jobs_due ON background_jobs(next_attempt_at, created_at)
    WHERE state IN ('PENDING', 'PROCESSING');
