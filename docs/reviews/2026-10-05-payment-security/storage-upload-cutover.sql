-- REVIEW ONLY: not applied to production.
-- Run after backend/frontend authenticated upload cutover, or in a planned upload maintenance window.
BEGIN;
DROP POLICY IF EXISTS "Anon upload avatars" ON storage.objects;
DROP POLICY IF EXISTS "Anon upload org logo/cover" ON storage.objects;
COMMIT;
-- Verify no anon INSERT/UPDATE/DELETE/ALL policy remains; retain intended public media read.
SELECT policyname, roles, cmd, qual, with_check
FROM pg_policies WHERE schemaname = 'storage' AND tablename = 'objects';
