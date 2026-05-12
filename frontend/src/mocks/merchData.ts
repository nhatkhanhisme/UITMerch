// ─── Mock product data ───────────────────────────────────────────────────────
export interface MockProduct {
  id: string;
  name: string;
  orgName: string;
  category: string;
  price?: number; // undefined = free / event giveaway
  image: string;
  description: string;
  featured?: boolean;
}

export const MOCK_PRODUCTS: MockProduct[] = [
  {
    id: "1",
    name: "Sticker GDG",
    orgName: "Google Developer Groups UIT",
    category: "Phụ kiện",
    price: 15000,
    image: "https://placehold.co/400x400/92fbff/1a3a4a?font=montserrat&text=GDG",
    description: "Bộ sticker in logo GDG độc đáo, chống nước, dùng trang trí laptop hoặc bình nước.",
    featured: true,
  },
  {
    id: "2",
    name: "Áo Hoodie UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Thời trang",
    price: 380000,
    image: "https://placehold.co/400x400/f8e697/1a3a4a?font=montserrat&text=HOODIE",
    description: "Áo hoodie unisex chất liệu cotton cao cấp, in logo UIT nổi bật phía trước.",
    featured: true,
  },
  {
    id: "3",
    name: "Bình Nước UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Dụng cụ",
    price: 120000,
    image: "https://placehold.co/400x400/d4f0ff/1a3a4a?font=montserrat&text=BOTTLE",
    description: "Bình nước giữ nhiệt 500ml, in logo UIT, phù hợp mang theo mọi nơi.",
    featured: true,
  },
  {
    id: "4",
    name: "Nón Lưỡi Trai UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Thời trang",
    price: 95000,
    image: "https://placehold.co/400x400/ffe4b5/1a3a4a?font=montserrat&text=CAP",
    description: "Mũ lưỡi trai phối màu xanh-trắng thể hiện tinh thần UIT.",
  },
  {
    id: "5",
    name: "Tote Bag Canvas",
    orgName: "UIT Media & Design",
    category: "Túi xách",
    price: 75000,
    image: "https://placehold.co/400x400/e0ffe4/1a3a4a?font=montserrat&text=TOTE",
    description: "Túi tote canvas thân thiện môi trường, thiết kế tối giản bởi đội ngũ UMD.",
  },
  {
    id: "6",
    name: "Áo Thun CS2024",
    orgName: "CLB Khoa học Máy tính",
    category: "Thời trang",
    price: 220000,
    image: "https://placehold.co/400x400/ffe0f0/1a3a4a?font=montserrat&text=TSHIRT",
    description: "Áo thun kỉ niệm CS2024, in hình vi mạch điện nghệ thuật ở mặt lưng.",
  },
  {
    id: "7",
    name: "Keychain Rồng Vàng",
    orgName: "Google Developer Groups UIT",
    category: "Phụ kiện",
    price: 35000,
    image: "https://placehold.co/400x400/fff7cc/1a3a4a?font=montserrat&text=KEY",
    description: "Móc khóa hình rồng vàng enamel pin giới hạn, phát tại sự kiện GDG DevFest.",
  },
  {
    id: "8",
    name: "Notebook UIT 2025",
    orgName: "Hội Sinh viên UIT",
    category: "Dụng cụ",
    price: 55000,
    image: "https://placehold.co/400x400/e8e8ff/1a3a4a?font=montserrat&text=NOTE",
    description: "Sổ tay bìa cứng 200 trang, bìa in logo UIT, giấy 100gsm không lem mực.",
  },
  {
    id: "9",
    name: "Pin Badge Logo UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Phụ kiện",
    // No price → event giveaway
    image: "https://placehold.co/400x400/ffd6cc/1a3a4a?font=montserrat&text=BADGE",
    description: "Huy hiệu ghim áo logo UIT phát miễn phí tại Lễ chào tân sinh viên 2025.",
  },
  {
    id: "10",
    name: "Cốc Sứ GDG",
    orgName: "Google Developer Groups UIT",
    category: "Dụng cụ",
    price: 85000,
    image: "https://placehold.co/400x400/ccf5ff/1a3a4a?font=montserrat&text=MUG",
    description: "Cốc sứ cao cấp 350ml, thiết kế gradient xanh đặc trưng của GDG.",
  },
  {
    id: "11",
    name: "Dây Đeo Thẻ UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Phụ kiện",
    // No price → event giveaway
    image: "https://placehold.co/400x400/f0e4ff/1a3a4a?font=montserrat&text=STRAP",
    description: "Dây đeo thẻ sinh viên phát tại tuần sinh hoạt công dân đầu năm học.",
  },
  {
    id: "12",
    name: "Áo Khoác Dù UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Thời trang",
    price: 450000,
    image: "https://placehold.co/400x400/dcffe8/1a3a4a?font=montserrat&text=JACKET",
    description: "Áo khoác dù 2 lớp chống gió, thêu logo UIT tại ngực trái, có túi kéo khóa.",
  },
];

export const FEATURED_PRODUCTS = MOCK_PRODUCTS.filter((p) => p.featured);
