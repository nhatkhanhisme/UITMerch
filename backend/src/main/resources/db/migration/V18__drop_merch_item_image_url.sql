-- Migrate any legacy single image_url into merch_images before dropping the column
INSERT INTO merch_images (id, merch_id, url, position)
SELECT gen_random_uuid(), id, image_url, 0
FROM merch_items
WHERE image_url IS NOT NULL
  AND id NOT IN (SELECT DISTINCT merch_id FROM merch_images);

ALTER TABLE merch_items DROP COLUMN image_url;
