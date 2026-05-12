-- V14__seed_demo_users_events_and_transactions.sql
-- Demo data: 1 admin, 5 customers, 5 events, event-merch links,
-- 3 active carts, 8 orders (various statuses), 3 wishlists
-- Default password for ALL seeded accounts: UIT@2025

DO $$
DECLARE
    -- BCrypt hash of "UIT@2025" (cost 10)
    pw TEXT := '$2a$10$bJMeGHBL0q8J4kceezsn7uR48Kcm45xd2UpNwzJzJkJCh2e7hJPqe';

    -- New user IDs
    u_admin  UUID;
    u_cust1  UUID;  -- Nguyen Van An
    u_cust2  UUID;  -- Tran Thi Bich
    u_cust3  UUID;  -- Le Minh Cuong
    u_cust4  UUID;  -- Pham Hong Duc
    u_cust5  UUID;  -- Hoang Thu Em

    -- Org IDs (resolved by owner email)
    o_cs        UUID;
    o_uitstore  UUID;
    o_nhipxuan  UUID;
    o_bait      UUID;
    o_xungkich  UUID;
    o_ce        UUID;
    o_nductxanh UUID;
    o_maytinhcu UUID;

    -- Merch IDs (resolved by name)
    m_ao_khoa_cs    UUID;
    m_day_deo_cs    UUID;
    m_moc_khoa_cs   UUID;
    m_sticker_miu   UUID;
    m_vong_tay_cs   UUID;
    m_moc_yoyo_cs   UUID;
    m_ao_thun_uit   UUID;
    m_balo_uit      UUID;
    m_binh_nuoc_uit UUID;
    m_day_deo_uit   UUID;
    m_mu_lui_trai   UUID;
    m_moc_ram       UUID;
    m_moc_cpu       UUID;
    m_ao_khoa_fce   UUID;
    m_nhan_kem      UUID;
    m_moc_kem       UUID;
    m_kep_toc_kem   UUID;
    m_bo_hoa_kem    UUID;
    m_kep_toc_hm    UUID;
    m_tui_len       UUID;
    m_moc_len       UUID;
    m_cot_toc       UUID;
    m_ao_ise        UUID;
    m_moc_ms        UUID;
    m_chau_sen      UUID;
    m_moc_mkx       UUID;
    m_bao_li_xi     UUID;
    m_sticker_nx    UUID;

    -- Event IDs
    ev_cs_welcome UUID;
    ev_uit_fest   UUID;
    ev_xuan_tn    UUID;
    ev_bait_sach  UUID;
    ev_mhx_2025   UUID;

    -- Cart IDs
    cart1 UUID;
    cart2 UUID;
    cart3 UUID;

    -- Order IDs
    ord1 UUID; ord2 UUID; ord3 UUID; ord4 UUID;
    ord5 UUID; ord6 UUID; ord7 UUID; ord8 UUID;

    -- Wishlist IDs
    wl1 UUID; wl2 UUID; wl3 UUID;

BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email = 'admin@uitmerch.edu.vn') THEN
        RETURN;
    END IF;

    -- ── 1. Admin user ────────────────────────────────────────────────────────
    INSERT INTO users (email, password_hash, full_name, role, is_verified)
        VALUES ('admin@uitmerch.edu.vn', pw, 'UITMerch Administrator', 'ADMIN', TRUE)
        RETURNING id INTO u_admin;

    -- ── 2. Customer users ────────────────────────────────────────────────────
    INSERT INTO users (email, password_hash, full_name, role, is_verified, phone, address)
        VALUES ('nguyen.van.an@student.uit.edu.vn', pw, 'Nguyễn Văn An',
                'CUSTOMER', TRUE, '0912345678',
                '123 Hàn Thuyên, Phước Long B, TP.HCM')
        RETURNING id INTO u_cust1;

    INSERT INTO users (email, password_hash, full_name, role, is_verified, phone, address)
        VALUES ('tran.thi.bich@student.uit.edu.vn', pw, 'Trần Thị Bích',
                'CUSTOMER', TRUE, '0987654321',
                '456 Lê Văn Việt, Tăng Nhơn Phú A, TP.HCM')
        RETURNING id INTO u_cust2;

    INSERT INTO users (email, password_hash, full_name, role, is_verified, phone, address)
        VALUES ('le.minh.cuong@student.uit.edu.vn', pw, 'Lê Minh Cường',
                'CUSTOMER', TRUE, '0901112233',
                '789 Nguyễn Xiển, Long Bình, TP.HCM')
        RETURNING id INTO u_cust3;

    INSERT INTO users (email, password_hash, full_name, role, is_verified, phone, address)
        VALUES ('pham.hong.duc@gmail.com', pw, 'Phạm Hồng Đức',
                'CUSTOMER', TRUE, '0933445566',
                '12 Trường Chinh, Tân Bình, TP.HCM')
        RETURNING id INTO u_cust4;

    INSERT INTO users (email, password_hash, full_name, role, is_verified, phone, address)
        VALUES ('hoang.thu.em@gmail.com', pw, 'Hoàng Thu Em',
                'CUSTOMER', TRUE, '0977889900',
                '45 Cách Mạng Tháng 8, Q.3, TP.HCM')
        RETURNING id INTO u_cust5;

    -- ── 3. Resolve Org IDs ───────────────────────────────────────────────────
    SELECT id INTO o_cs         FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'cs.khmt@uit.edu.vn');
    SELECT id INTO o_uitstore   FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'uitstore@uit.edu.vn');
    SELECT id INTO o_nhipxuan   FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'nhipxuan@uit.edu.vn');
    SELECT id INTO o_bait       FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'bait@uit.edu.vn');
    SELECT id INTO o_xungkich   FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'xungkich.truyen.thong@uit.edu.vn');
    SELECT id INTO o_ce         FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'fce@uit.edu.vn');
    SELECT id INTO o_nductxanh  FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'ngon.duoc.xanh@uit.edu.vn');
    SELECT id INTO o_maytinhcu  FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'maytinhcu@uit.edu.vn');

    -- ── 4. Resolve Merch IDs ─────────────────────────────────────────────────
    SELECT id INTO m_ao_khoa_cs     FROM merch_items WHERE name = 'Áo khoa CS';
    SELECT id INTO m_day_deo_cs     FROM merch_items WHERE name = 'Dây đeo CS';
    SELECT id INTO m_moc_khoa_cs    FROM merch_items WHERE name = 'Móc khóa CS';
    SELECT id INTO m_sticker_miu    FROM merch_items WHERE name = 'Sticker Miu';
    SELECT id INTO m_vong_tay_cs    FROM merch_items WHERE name = 'Vòng tay CS';
    SELECT id INTO m_moc_yoyo_cs    FROM merch_items WHERE name = 'Móc dây yoyo CS';
    SELECT id INTO m_ao_thun_uit    FROM merch_items WHERE name = 'Áo thun UIT';
    SELECT id INTO m_balo_uit       FROM merch_items WHERE name = 'Balo UIT';
    SELECT id INTO m_binh_nuoc_uit  FROM merch_items WHERE name = 'Bình nước UIT';
    SELECT id INTO m_day_deo_uit    FROM merch_items WHERE name = 'Dây đeo UIT';
    SELECT id INTO m_mu_lui_trai    FROM merch_items WHERE name = 'Mũ lưỡi trai UIT';
    SELECT id INTO m_moc_ram        FROM merch_items WHERE name = 'Móc khóa RAM';
    SELECT id INTO m_moc_cpu        FROM merch_items WHERE name = 'Móc khóa CPU';
    SELECT id INTO m_ao_khoa_fce    FROM merch_items WHERE name = 'Áo khoa Kỹ thuật Máy tính';
    SELECT id INTO m_nhan_kem       FROM merch_items WHERE name = 'Nhẫn kẽm nhung';
    SELECT id INTO m_moc_kem        FROM merch_items WHERE name = 'Móc khóa kẽm nhung';
    SELECT id INTO m_kep_toc_kem    FROM merch_items WHERE name = 'Kẹp tóc kẽm nhung';
    SELECT id INTO m_bo_hoa_kem     FROM merch_items WHERE name = 'Bó hoa kẽm nhung';
    SELECT id INTO m_kep_toc_hm     FROM merch_items WHERE name = 'Kẹp tóc handmade';
    SELECT id INTO m_tui_len        FROM merch_items WHERE name = 'Túi len handmade';
    SELECT id INTO m_moc_len        FROM merch_items WHERE name = 'Móc khóa len';
    SELECT id INTO m_cot_toc        FROM merch_items WHERE name = 'Cột tóc handmade';
    SELECT id INTO m_ao_ise         FROM merch_items WHERE name = 'Áo khoa KH&KTTT (ISE)';
    SELECT id INTO m_moc_ms         FROM merch_items WHERE name = 'Móc khóa MonoSunshine';
    SELECT id INTO m_chau_sen       FROM merch_items WHERE name = 'Chậu sen đá';
    SELECT id INTO m_moc_mkx        FROM merch_items WHERE name = 'Móc khóa Mùa Hè Xanh 2025';
    SELECT id INTO m_bao_li_xi      FROM merch_items WHERE name = 'Bao lì xì Nhịp Xuân';
    SELECT id INTO m_sticker_nx     FROM merch_items WHERE name = 'Sticker Nhịp Xuân phiên bản giới hạn';

    -- ── 5. Events ────────────────────────────────────────────────────────────

    -- CS: welcome week (PUBLISHED, past – good demo data)
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_cs,
                'Tuần lễ Chào Tân Sinh Viên 2025',
                'Tuần lễ chào đón tân sinh viên khoa CS 2025. Mua ấn phẩm khoa CS với giá ưu đãi đặc biệt – áo khoa, sticker, móc khóa, dây đeo và nhiều quà tặng kỷ niệm.',
                'PUBLISHED', '2025-09-01 08:00:00', '2025-09-07 18:00:00')
        RETURNING id INTO ev_cs_welcome;

    -- UIT Store: Tech Festival (PUBLISHED)
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_uitstore,
                'UIT Tech Festival 2025',
                'Sự kiện công nghệ thường niên của Trường ĐH Công nghệ Thông tin. Mua sắm ấn phẩm thương hiệu UIT chính hãng với nhiều combo ưu đãi hấp dẫn.',
                'PUBLISHED', '2025-11-15 08:00:00', '2025-11-17 20:00:00')
        RETURNING id INTO ev_uit_fest;

    -- Nhip Xuan: Volunteer fundraiser (DRAFT – upcoming)
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_nhipxuan,
                'Xuân Tình Nguyện UIT 2026',
                'Chương trình tình nguyện mùa Xuân 2026 – gây quỹ học bổng cho sinh viên có hoàn cảnh khó khăn. Mua bao lì xì và sticker phiên bản giới hạn để ủng hộ chương trình ý nghĩa này.',
                'DRAFT', '2026-01-20 08:00:00', '2026-01-25 18:00:00')
        RETURNING id INTO ev_xuan_tn;

    -- BAIT: Book Fair (ENDED)
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_bait,
                'Ngày Hội Sách BAIT 2025',
                'Ngày hội sách thường niên của CLB Sách và Hành động UIT (BAIT). Doanh thu từ bán ấn phẩm được dùng để tổ chức các hoạt động đọc sách và chia sẻ tri thức trong cộng đồng UIT.',
                'ENDED', '2025-04-01 08:00:00', '2025-04-03 17:00:00')
        RETURNING id INTO ev_bait_sach;

    -- Xung Kich: Mua He Xanh fundraiser (PUBLISHED)
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_xungkich,
                'Mùa Hè Xanh 2025 – Gây Quỹ & Tuyển Quân',
                'Đội hình Truyền Thông – Xung Kích UIT phát động chiến dịch gây quỹ Mùa Hè Xanh 2025. Mua móc khóa kỷ niệm MHX 2025 để ủng hộ các bạn tình nguyện viên tại vùng sâu vùng xa.',
                'PUBLISHED', '2025-06-01 08:00:00', '2025-06-30 18:00:00')
        RETURNING id INTO ev_mhx_2025;

    -- ── 6. Event–Merch links (merch from the same org as the event) ──────────
    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev_cs_welcome, m_ao_khoa_cs),
        (ev_cs_welcome, m_day_deo_cs),
        (ev_cs_welcome, m_moc_khoa_cs),
        (ev_cs_welcome, m_sticker_miu),
        (ev_cs_welcome, m_vong_tay_cs),
        (ev_cs_welcome, m_moc_yoyo_cs);

    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev_uit_fest, m_ao_thun_uit),
        (ev_uit_fest, m_balo_uit),
        (ev_uit_fest, m_binh_nuoc_uit),
        (ev_uit_fest, m_day_deo_uit),
        (ev_uit_fest, m_mu_lui_trai);

    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev_xuan_tn, m_bao_li_xi),
        (ev_xuan_tn, m_sticker_nx);

    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev_bait_sach, m_moc_ms),
        (ev_bait_sach, m_chau_sen);

    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev_mhx_2025, m_moc_mkx);

    -- ── 7. Active carts ──────────────────────────────────────────────────────

    -- Nguyen Van An – CS items
    INSERT INTO carts (user_id, status) VALUES (u_cust1, 'ACTIVE') RETURNING id INTO cart1;
    INSERT INTO cart_items (cart_id, merch_id, quantity) VALUES
        (cart1, m_ao_khoa_cs,  1),
        (cart1, m_day_deo_cs,  2),
        (cart1, m_sticker_miu, 1);

    -- Tran Thi Bich – UIT Store items
    INSERT INTO carts (user_id, status) VALUES (u_cust2, 'ACTIVE') RETURNING id INTO cart2;
    INSERT INTO cart_items (cart_id, merch_id, quantity) VALUES
        (cart2, m_ao_thun_uit,  2),
        (cart2, m_balo_uit,     1),
        (cart2, m_binh_nuoc_uit,1);

    -- Le Minh Cuong – mixed
    INSERT INTO carts (user_id, status) VALUES (u_cust3, 'ACTIVE') RETURNING id INTO cart3;
    INSERT INTO cart_items (cart_id, merch_id, quantity) VALUES
        (cart3, m_moc_khoa_cs, 2),
        (cart3, m_moc_ram,     3),
        (cart3, m_kep_toc_hm,  1);

    -- ── 8. Orders ────────────────────────────────────────────────────────────

    -- Ord 1: Nguyen Van An – CS – SUCCESS / PAID
    --   Ao khoa CS (1×190000) + Sticker Miu (2×15000) = 220000
    INSERT INTO orders (user_id, org_id, total_amount, status, payment_method, payment_status, note)
        VALUES (u_cust1, o_cs, 220000, 'SUCCESS', 'CASH_ON_DELIVERY', 'PAID',
                'Giao tới phòng 203 nhà B, sau 17 giờ')
        RETURNING id INTO ord1;
    INSERT INTO order_items (order_id, merch_id, merch_name, unit_price, quantity, subtotal) VALUES
        (ord1, m_ao_khoa_cs,  'Áo khoa CS',   190000, 1, 190000),
        (ord1, m_sticker_miu, 'Sticker Miu',   15000, 2,  30000);

    -- Ord 2: Tran Thi Bich – UIT Store – SUCCESS / PAID
    --   Ao thun UIT (1×155000) + Day deo UIT (2×35000) = 225000
    INSERT INTO orders (user_id, org_id, total_amount, status, payment_method, payment_status)
        VALUES (u_cust2, o_uitstore, 225000, 'SUCCESS', 'CASH_ON_DELIVERY', 'PAID')
        RETURNING id INTO ord2;
    INSERT INTO order_items (order_id, merch_id, merch_name, unit_price, quantity, subtotal) VALUES
        (ord2, m_ao_thun_uit,  'Áo thun UIT',   155000, 1, 155000),
        (ord2, m_day_deo_uit,  'Dây đeo UIT',    35000, 2,  70000);

    -- Ord 3: Guest – UIT Store – CONFIRMED / PENDING
    --   Balo UIT (1×490000) + Binh nuoc UIT (1×150000) = 640000
    INSERT INTO orders (org_id,
                        guest_name, guest_email, guest_phone, guest_address,
                        total_amount, status, payment_method, payment_status)
        VALUES (o_uitstore,
                'Đinh Quốc Khải', 'dinh.quoc.khai@gmail.com', '0908123456',
                '88 Hoàng Diệu 2, Linh Chiểu, TP.HCM',
                640000, 'CONFIRMED', 'CASH_ON_DELIVERY', 'PENDING')
        RETURNING id INTO ord3;
    INSERT INTO order_items (order_id, merch_id, merch_name, unit_price, quantity, subtotal) VALUES
        (ord3, m_balo_uit,      'Balo UIT',       490000, 1, 490000),
        (ord3, m_binh_nuoc_uit, 'Bình nước UIT',  150000, 1, 150000);

    -- Ord 4: Le Minh Cuong – May Tinh Cu – PENDING / PENDING
    --   Moc khoa RAM (3×20000) + Moc khoa CPU (2×30000) = 120000
    INSERT INTO orders (user_id, org_id, total_amount, status, payment_method, payment_status, note)
        VALUES (u_cust3, o_maytinhcu, 120000, 'PENDING', 'CASH_ON_DELIVERY', 'PENDING',
                'Combo 5 móc – mua nhiều giảm thêm, liên hệ trực tiếp')
        RETURNING id INTO ord4;
    INSERT INTO order_items (order_id, merch_id, merch_name, unit_price, quantity, subtotal) VALUES
        (ord4, m_moc_ram, 'Móc khóa RAM', 20000, 3, 60000),
        (ord4, m_moc_cpu, 'Móc khóa CPU', 30000, 2, 60000);

    -- Ord 5: Pham Hong Duc – UIT Store – READY_FOR_PICKUP / PAID
    --   Mu lui trai UIT (1×100000) = 100000
    INSERT INTO orders (user_id, org_id, total_amount, status, payment_method, payment_status)
        VALUES (u_cust4, o_uitstore, 100000, 'READY_FOR_PICKUP', 'CASH_ON_DELIVERY', 'PAID')
        RETURNING id INTO ord5;
    INSERT INTO order_items (order_id, merch_id, merch_name, unit_price, quantity, subtotal) VALUES
        (ord5, m_mu_lui_trai, 'Mũ lưỡi trai UIT', 100000, 1, 100000);

    -- Ord 6: Hoang Thu Em – FCE – SUCCESS / PAID
    --   Ao khoa FCE (2×150000) = 300000
    INSERT INTO orders (user_id, org_id, total_amount, status, payment_method, payment_status, note)
        VALUES (u_cust5, o_ce, 300000, 'SUCCESS', 'CASH_ON_DELIVERY', 'PAID',
                'Combo 2 áo, mua cho bạn cùng phòng')
        RETURNING id INTO ord6;
    INSERT INTO order_items (order_id, merch_id, merch_name, unit_price, quantity, subtotal) VALUES
        (ord6, m_ao_khoa_fce, 'Áo khoa Kỹ thuật Máy tính', 150000, 2, 300000);

    -- Ord 7: Nguyen Van An – BAIT – SUCCESS / PAID (bought during ENDED event)
    --   Moc khoa MonoSunshine (3×10000) + Chau sen da (1×20000) = 50000
    INSERT INTO orders (user_id, org_id, total_amount, status, payment_method, payment_status)
        VALUES (u_cust1, o_bait, 50000, 'SUCCESS', 'CASH_ON_DELIVERY', 'PAID')
        RETURNING id INTO ord7;
    INSERT INTO order_items (order_id, merch_id, merch_name, unit_price, quantity, subtotal) VALUES
        (ord7, m_moc_ms,   'Móc khóa MonoSunshine', 10000, 3, 30000),
        (ord7, m_chau_sen, 'Chậu sen đá',            20000, 1, 20000);

    -- Ord 8: Guest – CS – CANCELLED / FAILED
    --   Vong tay CS (1×30000) = 30000
    INSERT INTO orders (org_id,
                        guest_name, guest_email, guest_phone, guest_address,
                        total_amount, status, payment_method, payment_status, note)
        VALUES (o_cs,
                'Mai Thị Hoa', 'mai.thi.hoa@gmail.com', '0922334455',
                '15 Lê Trọng Tấn, Tây Thạnh, Tân Phú, TP.HCM',
                30000, 'CANCELLED', 'CASH_ON_DELIVERY', 'FAILED',
                'Khách hủy đơn – hết hàng')
        RETURNING id INTO ord8;
    INSERT INTO order_items (order_id, merch_id, merch_name, unit_price, quantity, subtotal) VALUES
        (ord8, m_vong_tay_cs, 'Vòng tay CS', 30000, 1, 30000);

    -- ── 9. Wishlists ─────────────────────────────────────────────────────────

    -- Nguyen Van An
    INSERT INTO wishlists (user_id) VALUES (u_cust1) RETURNING id INTO wl1;
    INSERT INTO wishlist_items (wishlist_id, merch_id) VALUES
        (wl1, m_balo_uit),
        (wl1, m_ao_thun_uit),
        (wl1, m_moc_mkx);

    -- Tran Thi Bich
    INSERT INTO wishlists (user_id) VALUES (u_cust2) RETURNING id INTO wl2;
    INSERT INTO wishlist_items (wishlist_id, merch_id) VALUES
        (wl2, m_ao_khoa_cs),
        (wl2, m_vong_tay_cs),
        (wl2, m_kep_toc_hm),
        (wl2, m_tui_len);

    -- Le Minh Cuong
    INSERT INTO wishlists (user_id) VALUES (u_cust3) RETURNING id INTO wl3;
    INSERT INTO wishlist_items (wishlist_id, merch_id) VALUES
        (wl3, m_ao_khoa_fce),
        (wl3, m_ao_ise),
        (wl3, m_mu_lui_trai);

END $$;
