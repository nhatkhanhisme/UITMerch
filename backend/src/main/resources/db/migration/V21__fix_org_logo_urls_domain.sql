-- V21__fix_org_logo_urls_domain.sql
-- Fix logo_url domain: .supabase.com → .supabase.co (correct Supabase project URL)

UPDATE organizations
SET logo_url = REPLACE(logo_url, '.supabase.com', '.supabase.co')
WHERE logo_url LIKE '%aubfixpblwlpsfgmwmza.supabase.com%';
