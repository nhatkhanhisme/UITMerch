-- V22__seed_vo_thuat_logo.sql
-- Set logo_url for CLB Võ thuật UIT

UPDATE organizations
SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/562da3fd-b2c5-4ad3-83ad-1cf77d779b93/logo/uma.jpg'
WHERE id = '562da3fd-b2c5-4ad3-83ad-1cf77d779b93';
