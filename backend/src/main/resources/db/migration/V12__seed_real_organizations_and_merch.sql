-- V12__seed_real_organizations_and_merch.sql
-- Real UIT club/organization data sourced from data/data.md
-- Default password for all organizer accounts: UIT@2025

DO $$
DECLARE
    pw  TEXT := '$2a$10$bJMeGHBL0q8J4kceezsn7uR48Kcm45xd2UpNwzJzJkJCh2e7hJPqe';

    u_cs          UUID; u_vidanem   UUID; u_maytinhcu UUID; u_nhipxuan  UUID;
    u_uitstore    UUID; u_xungkich  UUID; u_ce        UUID; u_cnpm      UUID;
    u_nductxanh   UUID; u_vixuan5   UUID; u_handmade  UUID; u_ise       UUID;
    u_sacxanh2    UUID; u_bait      UUID;

    o_cs          UUID; o_vidanem   UUID; o_maytinhcu UUID; o_nhipxuan  UUID;
    o_uitstore    UUID; o_xungkich  UUID; o_ce        UUID; o_cnpm      UUID;
    o_nductxanh   UUID; o_vixuan5   UUID; o_handmade  UUID; o_ise       UUID;
    o_sacxanh2    UUID; o_bait      UUID;
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email = 'cs.khmt@uit.edu.vn') THEN
        RETURN;
    END IF;

    -- ── Organizer users ───────────────────────────────────────────────────────
    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('cs.khmt@uit.edu.vn',          pw, 'Đoàn – Hội khoa Khoa học Máy tính',       'ORGANIZER', TRUE)
        RETURNING id INTO u_cs;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('vidanem@uit.edu.vn',           pw, 'Đội hình Vì Đàn Em',                       'ORGANIZER', TRUE)
        RETURNING id INTO u_vidanem;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('maytinhcu@uit.edu.vn',         pw, 'Đội hình Máy Tính Cũ – Tri Thức Mới',     'ORGANIZER', TRUE)
        RETURNING id INTO u_maytinhcu;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('nhipxuan@uit.edu.vn',          pw, 'Đội hình Nhịp Xuân',                       'ORGANIZER', TRUE)
        RETURNING id INTO u_nhipxuan;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('uitstore@uit.edu.vn',          pw, 'UIT Store',                                 'ORGANIZER', TRUE)
        RETURNING id INTO u_uitstore;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('xungkich.truyen.thong@uit.edu.vn', pw, 'Đội hình Truyền Thông – Xung Kích',   'ORGANIZER', TRUE)
        RETURNING id INTO u_xungkich;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('fce@uit.edu.vn',               pw, 'Khoa Kỹ thuật Máy tính',                  'ORGANIZER', TRUE)
        RETURNING id INTO u_ce;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('se@uit.edu.vn',                pw, 'Khoa Công nghệ Phần mềm',                  'ORGANIZER', TRUE)
        RETURNING id INTO u_cnpm;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('ngon.duoc.xanh@uit.edu.vn',    pw, 'Ban Xung kích – Ngọn Đuốc Xanh 2',        'ORGANIZER', TRUE)
        RETURNING id INTO u_nductxanh;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('vixuan5@uit.edu.vn',           pw, 'Vị Xuân 5 – Đội hình thường trực Đồng Tháp', 'ORGANIZER', TRUE)
        RETURNING id INTO u_vixuan5;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('handmade.xtn@uit.edu.vn',      pw, 'Đội hình Handmade – Xuân Tình Nguyện 2024', 'ORGANIZER', TRUE)
        RETURNING id INTO u_handmade;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('ise@uit.edu.vn',               pw, 'Khoa Khoa học & Kỹ thuật Thông tin',       'ORGANIZER', TRUE)
        RETURNING id INTO u_ise;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('sacxanh2@uit.edu.vn',          pw, 'Sắc Xanh 2 – Đội hình thường trực Bình Thuận', 'ORGANIZER', TRUE)
        RETURNING id INTO u_sacxanh2;

    INSERT INTO users (email, password_hash, full_name, role, is_verified) VALUES
        ('bait@uit.edu.vn',              pw, 'CLB Sách và Hành động UIT (BAIT)',          'ORGANIZER', TRUE)
        RETURNING id INTO u_bait;

    -- ── Organizations ─────────────────────────────────────────────────────────
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_cs,        'Đoàn – Hội khoa Khoa học Máy tính',        'Tổ chức Đoàn – Hội của Khoa Khoa học Máy tính (CS), UIT',            'ACTIVE') RETURNING id INTO o_cs;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_vidanem,   'Đội hình Vì Đàn Em',                        'Đội hình tình nguyện gây quỹ Vì Đàn Em, UIT',                        'ACTIVE') RETURNING id INTO o_vidanem;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_maytinhcu, 'Đội hình Máy Tính Cũ – Tri Thức Mới',       'Đội hình tình nguyện tái chế máy tính, gây quỹ học bổng, UIT',       'ACTIVE') RETURNING id INTO o_maytinhcu;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_nhipxuan,  'Đội hình Nhịp Xuân',                         'Đội hình tình nguyện Xuân – Xuân Tình Nguyện UIT',                   'ACTIVE') RETURNING id INTO o_nhipxuan;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_uitstore,  'UIT Store',                                   'Cửa hàng sản phẩm thương hiệu chính thức của Trường ĐH CNTT',        'ACTIVE') RETURNING id INTO o_uitstore;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_xungkich,  'Đội hình Truyền Thông – Xung Kích',          'Đội hình truyền thông xung kích UIT – Mùa Hè Xanh',                 'ACTIVE') RETURNING id INTO o_xungkich;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_ce,        'Khoa Kỹ thuật Máy tính (FCE)',                'Khoa Kỹ thuật Máy tính – Trường ĐH Công nghệ Thông tin',             'ACTIVE') RETURNING id INTO o_ce;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_cnpm,      'Khoa Công nghệ Phần mềm (SE)',                'Khoa Công nghệ Phần mềm – Trường ĐH Công nghệ Thông tin',            'ACTIVE') RETURNING id INTO o_cnpm;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_nductxanh, 'Ban Xung kích – Ngọn Đuốc Xanh 2',          'Ban Xung kích tình nguyện Ngọn Đuốc Xanh, UIT',                     'ACTIVE') RETURNING id INTO o_nductxanh;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_vixuan5,   'Vị Xuân 5 – Đội hình thường trực Đồng Tháp','Đội hình thường trực Đồng Tháp – Xuân Tình Nguyện UIT',             'ACTIVE') RETURNING id INTO o_vixuan5;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_handmade,  'Đội hình Handmade – Xuân Tình Nguyện 2024',  'Đội hình Handmade gây quỹ Xuân Tình Nguyện 2024, UIT',              'ACTIVE') RETURNING id INTO o_handmade;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_ise,       'Khoa Khoa học & Kỹ thuật Thông tin (ISE)',   'Khoa Khoa học & Kỹ thuật Thông tin – Trường ĐH Công nghệ Thông tin','ACTIVE') RETURNING id INTO o_ise;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_sacxanh2,  'Sắc Xanh 2 – Đội hình thường trực Bình Thuận','Đội hình thường trực Bình Thuận – Mùa Hè Xanh UIT',              'ACTIVE') RETURNING id INTO o_sacxanh2;
    INSERT INTO organizations (owner_id, name, description, status) VALUES
        (u_bait,      'CLB Sách và Hành động UIT (BAIT)',            'Câu lạc bộ Sách và Hành động UIT',                                  'ACTIVE') RETURNING id INTO o_bait;

    -- ── Merch items ───────────────────────────────────────────────────────────

    -- CS KHMT: CS BOX items (row 17 prices) + CS BOX 2026 placeholder (row 1)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_cs, 'CS BOX 2026',          'Bộ ấn phẩm khoa CS năm 2026 – danh sách vật phẩm sẽ được cập nhật', 0,      0,  'DRAFT'),
        (o_cs, 'Áo khoa CS',           'Áo khoa chính thức của Khoa Khoa học Máy tính',                      190000, 50, 'PUBLISHED'),
        (o_cs, 'Dây đeo CS',           'Dây đeo thẻ sinh viên thiết kế riêng của khoa CS',                   40000,  50, 'PUBLISHED'),
        (o_cs, 'Móc dây yoyo CS',      'Móc dây yoyo in logo khoa CS',                                       25000,  30, 'PUBLISHED'),
        (o_cs, 'Móc khóa CS',          'Móc khóa thiết kế riêng của khoa CS',                                30000,  30, 'PUBLISHED'),
        (o_cs, 'Sticker Miu',          'Bộ sticker nhân vật Miu – linh vật khoa CS',                         15000, 100, 'PUBLISHED'),
        (o_cs, 'Vòng tay CS',          'Vòng tay thiết kế riêng của khoa CS',                                30000,  50, 'PUBLISHED');

    -- Đội hình Vì Đàn Em: price not disclosed (row 2)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_vidanem, 'Móc khóa bông hoa', 'Móc khóa hình bông hoa handmade – gây quỹ Xuân Tình Nguyện 2026', 0, 0, 'DRAFT'),
        (o_vidanem, 'Vòng tay',           'Vòng tay handmade – gây quỹ Xuân Tình Nguyện 2026',               0, 0, 'DRAFT');

    -- Đội hình Máy Tính Cũ (rows 3, 8)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_maytinhcu, 'Móc khóa RAM',  'Móc khóa hình thanh RAM – mua 3 trở lên giảm 10.000đ, từ 3 sp freeship', 20000, 50, 'PUBLISHED'),
        (o_maytinhcu, 'Móc khóa CPU',  'Móc khóa hình CPU – mua 3 trở lên giảm 10.000đ, từ 3 sp freeship',       30000, 50, 'PUBLISHED');

    -- Đội hình Nhịp Xuân (rows 4, 5, 9)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_nhipxuan, 'Bao lì xì Nhịp Xuân',              'Bao lì xì thiết kế riêng – xấp 5 bao, gây quỹ Xuân Tình Nguyện', 15000, 100, 'PUBLISHED'),
        (o_nhipxuan, 'Sticker Nhịp Xuân phiên bản giới hạn', 'Sticker giới hạn Nhịp Xuân / vé số may mắn đặc biệt',          10000, 100, 'PUBLISHED');

    -- UIT Store (rows 6, 12 – dùng giá mới nhất từ 2025)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_uitstore, 'Áo thun UIT',       'Áo thun chính hãng UIT – chào mừng tân sinh viên 2025',   155000, 100, 'PUBLISHED'),
        (o_uitstore, 'Dây đeo UIT',       'Dây đeo thẻ thương hiệu UIT',                              35000,  100, 'PUBLISHED'),
        (o_uitstore, 'Bình nước UIT',     'Bình nước thương hiệu UIT',                                150000,  50, 'PUBLISHED'),
        (o_uitstore, 'Mũ lưỡi trai UIT', 'Mũ lưỡi trai có thêu logo UIT',                           100000,  50, 'PUBLISHED'),
        (o_uitstore, 'Balo UIT',          'Balo chính hãng thương hiệu UIT',                          490000,  30, 'PUBLISHED');

    -- Đội hình Truyền Thông – Xung Kích (row 7)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_xungkich, 'Móc khóa Mùa Hè Xanh 2025', 'Móc khóa kỷ niệm Mùa Hè Xanh 2025 – combo 3 cái 40.000đ, combo 5 cái 60.000đ', 15000, 50, 'PUBLISHED');

    -- Khoa Kỹ thuật Máy tính / CE (row 10)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_ce, 'Áo khoa Kỹ thuật Máy tính', 'Áo khoa chính thức FCE 2024 – mua 2 áo chỉ 290.000đ', 150000, 50, 'PUBLISHED');

    -- Khoa CNPM (row 11 – giá không nêu rõ)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_cnpm, 'Áo khoa CNPM màu xanh',  'Áo khoa chính thức Khoa Công nghệ Phần mềm màu xanh', 0, 0, 'DRAFT'),
        (o_cnpm, 'Áo khoa CNPM màu trắng', 'Áo khoa chính thức Khoa Công nghệ Phần mềm màu trắng', 0, 0, 'DRAFT');

    -- Ban Xung kích – Ngọn Đuốc Xanh 2 (row 13)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_nductxanh, 'Nhẫn kẽm nhung',        'Nhẫn làm từ kẽm nhung – gây quỹ Ngọn Đuốc Xanh 2',       8000,  50, 'PUBLISHED'),
        (o_nductxanh, 'Móc khóa kẽm nhung',    'Móc khóa làm từ kẽm nhung – gây quỹ Ngọn Đuốc Xanh 2',  10000,  50, 'PUBLISHED'),
        (o_nductxanh, 'Kẹp tóc kẽm nhung',     'Kẹp tóc làm từ kẽm nhung – gây quỹ Ngọn Đuốc Xanh 2',  12000,  50, 'PUBLISHED'),
        (o_nductxanh, 'Bó hoa kẽm nhung',      'Bó hoa làm từ kẽm nhung – gây quỹ Ngọn Đuốc Xanh 2',   20000,  30, 'PUBLISHED');

    -- Vị Xuân 5 (row 14)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_vixuan5, 'Bao lì xì Giáp Thìn 2024', 'Set 5 bao lì xì thiết kế Xuân Giáp Thìn 2024 – gây quỹ tình nguyện', 12000, 50, 'PUBLISHED');

    -- Đội hình Handmade (row 15, range 10k–100k → giá đại diện từng sản phẩm)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_handmade, 'Kẹp tóc handmade',   'Kẹp tóc handmade – gây quỹ Xuân Tình Nguyện 2024',  10000,  50, 'PUBLISHED'),
        (o_handmade, 'Túi len handmade',    'Túi len handmade – gây quỹ Xuân Tình Nguyện 2024',  50000,  20, 'PUBLISHED'),
        (o_handmade, 'Móc khóa len',        'Móc khóa len handmade – gây quỹ Xuân Tình Nguyện 2024', 20000, 50, 'PUBLISHED'),
        (o_handmade, 'Cột tóc handmade',   'Cột tóc handmade – gây quỹ Xuân Tình Nguyện 2024',  15000,  50, 'PUBLISHED');

    -- Khoa ISE (row 16)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_ise, 'Áo khoa KH&KTTT (ISE)', 'Áo khoa chính thức Khoa Khoa học & Kỹ thuật Thông tin – pre-order 18–25/12/2023', 110000, 100, 'PUBLISHED');

    -- Sắc Xanh 2 (row 18 – giá không nêu rõ)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_sacxanh2, 'Hoa handmade Sắc Xanh 2', 'Hoa handmade – gây quỹ Mùa Hè Xanh, Đội hình thường trực Bình Thuận', 0, 0, 'DRAFT');

    -- CLB BAIT (row 19)
    INSERT INTO merch_items (org_id, name, description, price, stock, status) VALUES
        (o_bait, 'Móc khóa MonoSunshine', 'Móc khóa MonoSunshine – gây quỹ hoạt động CLB BAIT', 10000, 50, 'PUBLISHED'),
        (o_bait, 'Chậu sen đá',           'Chậu sen đá – gây quỹ hoạt động CLB BAIT',           20000, 30, 'PUBLISHED');

END $$;
