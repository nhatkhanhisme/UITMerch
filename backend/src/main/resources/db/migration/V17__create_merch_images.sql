CREATE TABLE merch_images (
    id          UUID         PRIMARY KEY,
    merch_id    UUID         NOT NULL REFERENCES merch_items(id) ON DELETE CASCADE,
    url         TEXT         NOT NULL,
    position    INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_merch_images_merch_id ON merch_images(merch_id);
