-- V13__create_categories.sql

CREATE TABLE categories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug          VARCHAR(100) UNIQUE NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO categories (slug, name, description, display_order) VALUES
    ('trang-phuc',        'Trang phục',           'Áo khoa, áo thun UIT, mũ lưỡi trai',              1),
    ('tui-balo',          'Túi & Balo',            'Balo UIT, túi len handmade',                       2),
    ('do-dung',           'Đồ dùng',               'Bình nước UIT',                                    3),
    ('phu-kien-ca-nhan',  'Phụ kiện cá nhân',      'Dây đeo thẻ, vòng tay, nhẫn, kẹp tóc, cột tóc',  4),
    ('luu-niem',          'Đồ lưu niệm',           'Móc khóa các loại, sticker',                      5),
    ('qua-tang-handmade', 'Quà tặng & Handmade',   'Bao lì xì, hoa, quà handmade, cây cảnh',          6),
    ('combo-hop-qua',     'Combo & Hộp quà',       'CS BOX 2023, CS BOX 2026',                         7);

ALTER TABLE merch_items
    ADD COLUMN category_id UUID REFERENCES categories(id) ON DELETE SET NULL;

-- Trang phục
UPDATE merch_items SET category_id = (SELECT id FROM categories WHERE slug = 'trang-phuc')
WHERE name IN (
    'Áo khoa CS',
    'Áo thun UIT',
    'Mũ lưỡi trai UIT',
    'Áo khoa Kỹ thuật Máy tính',
    'Áo khoa CNPM màu xanh',
    'Áo khoa CNPM màu trắng',
    'Áo khoa KH&KTTT (ISE)'
);

-- Túi & Balo
UPDATE merch_items SET category_id = (SELECT id FROM categories WHERE slug = 'tui-balo')
WHERE name IN ('Balo UIT', 'Túi len handmade');

-- Đồ dùng
UPDATE merch_items SET category_id = (SELECT id FROM categories WHERE slug = 'do-dung')
WHERE name IN ('Bình nước UIT');

-- Phụ kiện cá nhân
UPDATE merch_items SET category_id = (SELECT id FROM categories WHERE slug = 'phu-kien-ca-nhan')
WHERE name IN (
    'Dây đeo CS',
    'Vòng tay CS',
    'Vòng tay',
    'Dây đeo UIT',
    'Móc dây yoyo CS',
    'Nhẫn kẽm nhung',
    'Kẹp tóc kẽm nhung',
    'Kẹp tóc handmade',
    'Cột tóc handmade'
);

-- Đồ lưu niệm
UPDATE merch_items SET category_id = (SELECT id FROM categories WHERE slug = 'luu-niem')
WHERE name IN (
    'Móc khóa CS',
    'Sticker Miu',
    'Sticker Nhịp Xuân phiên bản giới hạn',
    'Móc khóa Mùa Hè Xanh 2025',
    'Móc khóa RAM',
    'Móc khóa CPU',
    'Móc khóa kẽm nhung',
    'Móc khóa len',
    'Móc khóa MonoSunshine',
    'Móc khóa bông hoa'
);

-- Quà tặng & Handmade
UPDATE merch_items SET category_id = (SELECT id FROM categories WHERE slug = 'qua-tang-handmade')
WHERE name IN (
    'Bao lì xì Nhịp Xuân',
    'Bao lì xì Giáp Thìn 2024',
    'Hoa handmade Sắc Xanh 2',
    'Bó hoa kẽm nhung',
    'Chậu sen đá'
);

-- Combo & Hộp quà
UPDATE merch_items SET category_id = (SELECT id FROM categories WHERE slug = 'combo-hop-qua')
WHERE name IN ('CS BOX 2026');
