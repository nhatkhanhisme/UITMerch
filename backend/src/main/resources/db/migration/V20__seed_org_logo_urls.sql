-- V20__seed_org_logo_urls.sql
-- Set logo_url for all organizations using images hosted on Supabase.
-- Upload images first: cd scripts && node upload-org-logos.mjs
-- Path pattern: org-assets/organizations/{org-id}/logo/{slug}

-- Trường UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/6a637d6f-5fe9-4d12-bad6-199761e5b899/logo/uit.png'
WHERE id = '6a637d6f-5fe9-4d12-bad6-199761e5b899';

-- Khoa Khoa học Máy tính
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/2dca50c1-4717-4523-bf40-26e41f5f2d28/logo/khmt-uit.png'
WHERE id = '2dca50c1-4717-4523-bf40-26e41f5f2d28';

-- Khoa Kỹ thuật Máy tính
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/1dbae61f-ed0b-4b28-a523-6e92c241e602/logo/ktmt-fce.jpg'
WHERE id = '1dbae61f-ed0b-4b28-a523-6e92c241e602';

-- Khoa Công nghệ Phần mềm
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/cebfa619-8787-46c9-9151-5bbc88787853/logo/se-uit.png'
WHERE id = 'cebfa619-8787-46c9-9151-5bbc88787853';

-- Khoa Khoa học và Kỹ thuật Thông tin
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/ad88bd53-a52a-4a56-a23e-d53e3183c2a2/logo/khkttt-ise.png'
WHERE id = 'ad88bd53-a52a-4a56-a23e-d53e3183c2a2';

-- Khoa Hệ thống Thông tin
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/c36ed407-657c-423b-880e-d4c862a3d844/logo/httt.png'
WHERE id = 'c36ed407-657c-423b-880e-d4c862a3d844';

-- Khoa Mạng máy tính và Truyền thông
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/a1cd7527-f704-47cd-b2fd-43a2b3da0321/logo/mmt-tt.png'
WHERE id = 'a1cd7527-f704-47cd-b2fd-43a2b3da0321';

-- UIT Store
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/4eb1edcf-9e4a-4912-85c4-80b133d02451/logo/uit.png'
WHERE id = '4eb1edcf-9e4a-4912-85c4-80b133d02451';

-- CLB Trí tuệ Nhân tạo UIT / AI Club UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/9bc44f87-e948-440b-8510-d8e4c6220f06/logo/cs-uit-ai.png'
WHERE id = '9bc44f87-e948-440b-8510-d8e4c6220f06';

-- CSAC – Computer Science Art Club
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/d8b44d0a-d7d8-4858-8a0c-fc5e0b656968/logo/csac.jpg'
WHERE id = 'd8b44d0a-d7d8-4858-8a0c-fc5e0b656968';

-- CEEC – Computer Engineering Embedded Club
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/b8110173-bad6-433e-98d5-9ff0c78d95e3/logo/ceec.jpg'
WHERE id = 'b8110173-bad6-433e-98d5-9ff0c78d95e3';

-- UIT IC Club
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/8de613f0-d15b-4f93-860a-64b7538839ae/logo/ic-uit.jpg'
WHERE id = '8de613f0-d15b-4f93-860a-64b7538839ae';

-- UIT GamApp Studios
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/69aa1606-d876-4c57-8259-dd30fcad4ede/logo/gamapp.jpg'
WHERE id = '69aa1606-d876-4c57-8259-dd30fcad4ede';

-- Google Developer Group on Campus – UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/539f5e72-39cc-4425-b001-28fc60fa00de/logo/gdgoc.jpg'
WHERE id = '539f5e72-39cc-4425-b001-28fc60fa00de';

-- WebDev Studios
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/23300c90-3205-4e2f-90ce-f610b49cddbf/logo/webdev.jpg'
WHERE id = '23300c90-3205-4e2f-90ce-f610b49cddbf';

-- Artistry – CLB Truyền thông Số UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/c8b881bf-1c39-4bfe-b8a2-8f6ada4fee35/logo/artistry.jpg'
WHERE id = 'c8b881bf-1c39-4bfe-b8a2-8f6ada4fee35';

-- UIT Media Club
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/bdd8200e-3671-4297-ba20-3b3bf4d00007/logo/uma.jpg'
WHERE id = 'bdd8200e-3671-4297-ba20-3b3bf4d00007';

-- LOSSLESS – CLB Văn nghệ Xung kích UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/a6ecfd83-8436-4a7a-b26f-1296ef337892/logo/lossless.jpg'
WHERE id = 'a6ecfd83-8436-4a7a-b26f-1296ef337892';

-- Đội Event – UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/678f73f3-eb46-4454-acd1-b69bffe609e5/logo/event-uit.jpg'
WHERE id = '678f73f3-eb46-4454-acd1-b69bffe609e5';

-- UIT Open English Club
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/3e7e579f-f318-45db-8b60-d4787ae252fd/logo/oec.jpg'
WHERE id = '3e7e579f-f318-45db-8b60-d4787ae252fd';

-- CLB Tiếng Nhật UIT – Wakame
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/6e4b0555-e8c0-4a0d-9ffd-0c0a6c9753b5/logo/wakame.jpg'
WHERE id = '6e4b0555-e8c0-4a0d-9ffd-0c0a6c9753b5';

-- CLB Lý luận Trẻ UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/f153f5da-81a7-4799-9768-9fa6d37def6f/logo/ly-luan-tre.jpg'
WHERE id = 'f153f5da-81a7-4799-9768-9fa6d37def6f';

-- CLB Sinh viên 5 tốt – UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/73539ddb-ce62-4785-b53d-007c36e64475/logo/sv5t.jpg'
WHERE id = '73539ddb-ce62-4785-b53d-007c36e64475';

-- Đội Công tác Xã hội UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/2dbce5ec-7aa2-4fc1-842a-cc2355844683/logo/ctxh.jpg'
WHERE id = '2dbce5ec-7aa2-4fc1-842a-cc2355844683';

-- CLB Lớp trưởng UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/d227dc34-9a46-4ca9-b136-51ecede1f5f5/logo/uit-leader.jpg'
WHERE id = 'd227dc34-9a46-4ca9-b136-51ecede1f5f5';

-- CLB Sách và Hành động UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/f6ddd4e5-42e6-4329-b7a8-01e051484198/logo/clb-sach-hanh-dong.jpg'
WHERE id = 'f6ddd4e5-42e6-4329-b7a8-01e051484198';

-- Đội Máy Tính Cũ – Tri Thức Mới
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/e4445bab-248d-4828-9daa-1cc0def8f796/logo/mtc-ttm.jpg'
WHERE id = 'e4445bab-248d-4828-9daa-1cc0def8f796';

-- UIT Basketball Club
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/3cd1e2ea-d828-49c0-ab20-3daa7cf374fa/logo/basketball.jpg'
WHERE id = '3cd1e2ea-d828-49c0-ab20-3daa7cf374fa';

-- UIT Badminton Club
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/0ba3db46-608c-4d81-b527-684b3dc07ddb/logo/badminton.jpg'
WHERE id = '0ba3db46-608c-4d81-b527-684b3dc07ddb';

-- CLB Futsal UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/f6ed9f2a-8f98-418b-80fe-c78814d4d859/logo/futsal.jpg'
WHERE id = 'f6ed9f2a-8f98-418b-80fe-c78814d4d859';

-- CLB Bóng chuyền UIT – UVC
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/6c55a805-de7f-46bf-b937-8d269a867669/logo/uvc.jpg'
WHERE id = '6c55a805-de7f-46bf-b937-8d269a867669';

-- CLB Taekwondo UIT
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/757b9763-f916-4963-b311-d691db0f8411/logo/taekwondo.jpg'
WHERE id = '757b9763-f916-4963-b311-d691db0f8411';

-- UIT Chess Club
UPDATE organizations SET logo_url = 'https://aubfixpblwlpsfgmwmza.supabase.co/storage/v1/object/public/org-assets/organizations/5a9643c0-55f3-4140-918c-80f050465133/logo/uit-chess.jpg'
WHERE id = '5a9643c0-55f3-4140-918c-80f050465133';
