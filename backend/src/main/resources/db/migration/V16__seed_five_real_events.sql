-- V16__seed_five_real_events.sql
-- Five real UIT events sourced from actual club/store announcements, with event_merch links.

DO $$
DECLARE
    o_uitstore  UUID;
    o_maytinhcu UUID;
    o_cs        UUID;
    o_handmade  UUID;

    m_ao_thun_uit   UUID;
    m_day_deo_uit   UUID;
    m_binh_nuoc_uit UUID;
    m_mu_lui_trai   UUID;
    m_balo_uit      UUID;
    m_moc_ram       UUID;
    m_moc_cpu       UUID;
    m_ao_khoa_cs    UUID;
    m_day_deo_cs    UUID;
    m_moc_yoyo_cs   UUID;
    m_moc_khoa_cs   UUID;
    m_sticker_miu   UUID;
    m_vong_tay_cs   UUID;
    m_kep_toc_hm    UUID;
    m_tui_len       UUID;
    m_moc_len       UUID;
    m_cot_toc       UUID;

    ev1 UUID; ev2 UUID; ev3 UUID; ev4 UUID; ev5 UUID;

BEGIN
    IF EXISTS (SELECT 1 FROM events WHERE title = 'UIT Store – Sản phẩm chào mừng Tân sinh viên K20 (2025)') THEN
        RETURN;
    END IF;

    -- ── Resolve org IDs ──────────────────────────────────────────────────────
    SELECT id INTO o_uitstore  FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'uitstore@uit.edu.vn');
    SELECT id INTO o_maytinhcu FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'maytinhcu@uit.edu.vn');
    SELECT id INTO o_cs        FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'cs.khmt@uit.edu.vn');
    SELECT id INTO o_handmade  FROM organizations WHERE owner_id = (SELECT id FROM users WHERE email = 'handmade.xtn@uit.edu.vn');

    -- ── Resolve merch IDs ────────────────────────────────────────────────────
    SELECT id INTO m_ao_thun_uit   FROM merch_items WHERE name = 'Áo thun UIT';
    SELECT id INTO m_day_deo_uit   FROM merch_items WHERE name = 'Dây đeo UIT';
    SELECT id INTO m_binh_nuoc_uit FROM merch_items WHERE name = 'Bình nước UIT';
    SELECT id INTO m_mu_lui_trai   FROM merch_items WHERE name = 'Mũ lưỡi trai UIT';
    SELECT id INTO m_balo_uit      FROM merch_items WHERE name = 'Balo UIT';
    SELECT id INTO m_moc_ram       FROM merch_items WHERE name = 'Móc khóa RAM';
    SELECT id INTO m_moc_cpu       FROM merch_items WHERE name = 'Móc khóa CPU';
    SELECT id INTO m_ao_khoa_cs    FROM merch_items WHERE name = 'Áo khoa CS';
    SELECT id INTO m_day_deo_cs    FROM merch_items WHERE name = 'Dây đeo CS';
    SELECT id INTO m_moc_yoyo_cs   FROM merch_items WHERE name = 'Móc dây yoyo CS';
    SELECT id INTO m_moc_khoa_cs   FROM merch_items WHERE name = 'Móc khóa CS';
    SELECT id INTO m_sticker_miu   FROM merch_items WHERE name = 'Sticker Miu';
    SELECT id INTO m_vong_tay_cs   FROM merch_items WHERE name = 'Vòng tay CS';
    SELECT id INTO m_kep_toc_hm    FROM merch_items WHERE name = 'Kẹp tóc handmade';
    SELECT id INTO m_tui_len       FROM merch_items WHERE name = 'Túi len handmade';
    SELECT id INTO m_moc_len       FROM merch_items WHERE name = 'Móc khóa len';
    SELECT id INTO m_cot_toc       FROM merch_items WHERE name = 'Cột tóc handmade';

    -- ── Events ───────────────────────────────────────────────────────────────

    -- Event 1: UIT Store – Chào mừng Tân sinh viên K20 (2025)
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_uitstore,
                'UIT Store – Sản phẩm chào mừng Tân sinh viên K20 (2025)',
                E'UIT Store hân hạnh giới thiệu bộ "nhận diện" sinh viên UIT dành riêng cho tân sinh viên K20.\n\nSản phẩm:\n- Áo thun UIT: 155.000đ – chất liệu xịn, đủ size S–3XL\n- Dây đeo UIT: 35.000đ – phụ kiện gắn liền với sinh viên UIT\n- Bình nước UIT (Lock&Lock màu xanh thương hiệu): 150.000đ\n\nĐịa điểm: UIT Store – Sảnh tòa nhà A (đối diện phòng A104)\nHotline: 0366.360.633',
                'ENDED', '2025-08-27 09:02:00', '2025-09-09 18:00:00')
        RETURNING id INTO ev1;

    -- Event 2: Đội hình Máy Tính Cũ – Giới thiệu móc khóa gây quỹ
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_maytinhcu,
                'Giới thiệu móc khóa gây quỹ – Máy Tính Cũ Tri Thức Mới',
                E'Đội Máy Tính Cũ – Tri Thức Mới hân hoan giới thiệu bộ sưu tập móc khóa độc đáo mang đậm chất công nghệ.\n\nSản phẩm:\n- Móc Khóa RAM: 20.000đ/sản phẩm\n- Móc Khóa CPU: 30.000đ/sản phẩm\n\nƯu đãi chiến dịch Mùa Hè Xanh:\n- Miễn phí giao hàng từ 3 sản phẩm (khu vực đô thị làng Đại học)\n- Giảm ngay 10.000đ khi mua từ 3 móc khóa trở lên\n\nĐịa chỉ: Phòng D105, Trường ĐH CNTT\nFanpage: facebook.com/maytinhcu.trithucmoi.uit\nĐội trưởng Nguyễn Ngọc Tâm: 094.666.3099 – Email: mtcttm.uit@gmail.com',
                'ENDED', '2024-06-01 08:00:00', '2024-08-31 18:00:00')
        RETURNING id INTO ev2;

    -- Event 3: UIT Store – Sản phẩm thương hiệu UIT (2024)
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_uitstore,
                'Sản phẩm thương hiệu UIT tại UIT Store (2024)',
                E'UIT Store tiếp tục cung cấp các sản phẩm thương hiệu UIT.\n\nSản phẩm:\n- Áo thun UIT: 150.000đ (đặt áo tại link.uit.edu.vn/Dat-ao-thun-UIT từ 11/4–22/4/2024)\n- Dây đeo thẻ: 35.000đ\n- Mũ lưỡi trai: 100.000đ\n- Balo branding UIT: 490.000đ (số lượng có hạn)\n\nĐịa điểm: Văn phòng Đoàn Hội trường (Tòa D, phòng D101)\nGiờ bán: 8:00–16:30, thứ 2 đến thứ 6\nHotline: 0366.360.633 – Anh Phi',
                'ENDED', '2024-01-01 08:00:00', '2024-12-31 16:30:00')
        RETURNING id INTO ev3;

    -- Event 4: CS BOX 2023 – Đoàn Hội khoa KHMT
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_cs,
                'Mở bán ấn phẩm khoa KHMT – CS BOX (2023)',
                E'Đoàn – Hội khoa Khoa học Máy tính thông báo ấn phẩm khoa CS BOX cực kỳ dễ thương đã quay trở lại!\n\nCS BOX bao gồm:\n- Áo khoa CS: 190.000đ\n- Dây đeo đặc trưng CS: 40.000đ\n- Móc dây yoyo: 25.000đ\n- Móc khoá CS: 30.000đ\n- Bộ sticker Miu: 15.000đ\n- Vòng tay CS: 30.000đ\n- Combo CS BOX: 300.000đ\n\nMinigame nhận mã giảm giá 10%: check-in tại quầy CS Store + đăng ảnh #CS_UIT #CS_BOX #CSWITHLOVE lên trang cá nhân.\n\nĐịa điểm: CS Store, tầng 5, toà nhà E, UIT\nGiờ mở cửa: 09:00–16:00 (trừ Chủ nhật)\nSản phẩm đã bán hết vào ngày 09/10/2023.',
                'ENDED', '2023-10-02 09:00:00', '2023-10-09 16:00:00')
        RETURNING id INTO ev4;

    -- Event 5: Đội hình Handmade – Gây quỹ Xuân Tình Nguyện 2024
    INSERT INTO events (org_id, title, description, status, starts_at, ends_at)
        VALUES (o_handmade,
                'Gây quỹ đội hình Handmade – Xuân Tình Nguyện 2024',
                E'Đội hình Handmade – Xuân Tình Nguyện 2024 giới thiệu các sản phẩm thủ công tinh tế, gây quỹ mang yêu thương đến những mảnh đời cơ nhỡ dịp Tết.\n\nSản phẩm kẽm nhung:\n- Kẹp tóc (hoa tulip, sao biển, gấu, cầu vồng, tai thỏ)\n\nSản phẩm len:\n- Túi len handmade (1 màu / 2 màu) – limited\n- Móc khóa len (túi phúc, dâu, hoa anh đào, ếch, súp lơ)\n- Cột tóc thỏ, kẹp tóc lá mầm\n\nGiá chỉ từ 10.000–100.000đ\nĐặt hàng: tinyurl.com/XTNUIT2024handmade hoặc ghé trực tiếp phòng C104, Tòa C, UIT\nThời gian: 09:00–16:00, thứ 2 đến thứ 6',
                'ENDED', '2024-01-16 09:00:00', '2024-01-26 16:00:00')
        RETURNING id INTO ev5;

    -- ── Event–Merch links ─────────────────────────────────────────────────────

    -- Event 1: UIT Store – Tân sinh viên K20
    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev1, m_ao_thun_uit),
        (ev1, m_day_deo_uit),
        (ev1, m_binh_nuoc_uit);

    -- Event 2: Máy Tính Cũ – Móc khóa gây quỹ
    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev2, m_moc_ram),
        (ev2, m_moc_cpu)

    -- Event 3: UIT Store – Sản phẩm thương hiệu 2024
    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev3, m_ao_thun_uit),
        (ev3, m_day_deo_uit),
        (ev3, m_mu_lui_trai),
        (ev3, m_balo_uit);

    -- Event 4: CS BOX 2023
    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev4, m_ao_khoa_cs),
        (ev4, m_day_deo_cs),
        (ev4, m_moc_yoyo_cs),
        (ev4, m_moc_khoa_cs),
        (ev4, m_sticker_miu),
        (ev4, m_vong_tay_cs);

    -- Event 5: Handmade – Xuân Tình Nguyện 2024
    INSERT INTO event_merch (event_id, merch_id) VALUES
        (ev5, m_kep_toc_hm),
        (ev5, m_tui_len),
        (ev5, m_moc_len),
        (ev5, m_cot_toc);

END $$;
