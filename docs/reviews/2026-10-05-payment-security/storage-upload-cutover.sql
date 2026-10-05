-- Applied to UITMerch project aubfixpblwlpsfgmwmza on 2026-10-06 after backend/frontend upload cutover.
-- Run after backend/frontend authenticated upload cutover, or in a planned upload maintenance window.
BEGIN;
DROP POLICY IF EXISTS "Anon upload avatars" ON storage.objects;
DROP POLICY IF EXISTS "Anon upload org logo/cover" ON storage.objects;
UPDATE storage.buckets
SET file_size_limit=10485760, allowed_mime_types=ARRAY['image/jpeg','image/png','image/webp','image/gif']
WHERE id IN ('avatars','org-assets','merch-images','org-logos');
COMMIT;
-- Verify no anon INSERT/UPDATE/DELETE/ALL policy remains; retain intended public media read.
SELECT policyname, roles, cmd, qual, with_check
FROM pg_policies WHERE schemaname = 'storage' AND tablename = 'objects';
