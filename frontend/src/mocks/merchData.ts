export interface ProductColorOption {
  name: string;
  value: string;
  image?: string;
}

export interface ProductDetailSection {
  title: string;
  content: string;
}

export interface MockProduct {
  id: string;
  name: string;
  orgName: string;
  category: string;
  price?: number;
  image: string;
  gallery: string[];
  description: string;
  featured?: boolean;
  colors: ProductColorOption[];
  sizeLabel: string;
  sizeOptions: string[];
  stock: number;
  material?: string;
  detailSections: ProductDetailSection[];
}

const productImage = (bg: string, text: string) =>
  `https://placehold.co/900x900/${bg}/1a3a4a?font=montserrat&text=${encodeURIComponent(text)}`;

export const MOCK_PRODUCTS: MockProduct[] = [
  {
    id: "1",
    name: "Sticker GDG",
    orgName: "Google Developer Groups UIT",
    category: "Phụ kiện",
    price: 15000,
    image: productImage("92fbff", "GDG"),
    gallery: [
      productImage("92fbff", "GDG"),
      productImage("c8fdff", "STICKER"),
      productImage("f8e697", "LAPTOP"),
    ],
    description:
      "Bộ sticker in logo GDG độc đáo, chống nước, dùng trang trí laptop hoặc bình nước.",
    featured: true,
    colors: [
      { name: "Aqua", value: "#92FBFF", image: productImage("92fbff", "GDG") },
      { name: "Gold", value: "#F8E697", image: productImage("f8e697", "GDG") },
    ],
    sizeLabel: "Kích thước",
    sizeOptions: ["Bộ 5 sticker"],
    stock: 48,
    material: "Decal chống nước phủ mờ",
    detailSections: [
      {
        title: "Thông tin sản phẩm",
        content:
          "Sticker GDG mang tinh thần công nghệ UIT lên những vật dụng quen thuộc hằng ngày. Thiết kế nổi bật, dễ nhận diện và phù hợp để trang trí laptop, bình nước hoặc góc học tập.",
      },
      {
        title: "Bảo quản",
        content:
          "Lau khô bề mặt trước khi dán. Tránh bóc dán nhiều lần để giữ lớp keo bền và đẹp.",
      },
    ],
  },
  {
    id: "2",
    name: "Áo Hoodie UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Thời trang",
    price: 380000,
    image: productImage("f8e697", "HOODIE"),
    gallery: [
      productImage("f8e697", "HOODIE"),
      productImage("fff3b8", "FRONT"),
      productImage("d4f0ff", "BACK"),
    ],
    description:
      "Áo hoodie unisex chất liệu cotton cao cấp, in logo UIT nổi bật phía trước.",
    featured: true,
    colors: [
      { name: "Cream", value: "#F8E697", image: productImage("f8e697", "HOODIE") },
      { name: "Sky", value: "#D4F0FF", image: productImage("d4f0ff", "HOODIE") },
      { name: "Black", value: "#131B2E", image: productImage("131b2e", "HOODIE") },
    ],
    sizeLabel: "Kích cỡ",
    sizeOptions: ["S", "M", "L", "XL"],
    stock: 18,
    material: "Cotton fleece ấm nhẹ",
    detailSections: [
      {
        title: "Form dáng",
        content:
          "Form unisex rộng vừa, dễ phối với quần jeans, kaki hoặc jogger. Phần bo tay và bo lai giữ dáng tốt khi mặc hằng ngày.",
      },
      {
        title: "Chất liệu",
        content:
          "Bề mặt vải mềm, lớp trong ấm nhẹ, phù hợp đi học, sinh hoạt câu lạc bộ và các sự kiện ngoài trời.",
      },
    ],
  },
  {
    id: "3",
    name: "Bình Nước UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Dụng cụ",
    price: 120000,
    image: productImage("d4f0ff", "BOTTLE"),
    gallery: [
      productImage("d4f0ff", "BOTTLE"),
      productImage("e9feff", "500ML"),
      productImage("92fbff", "UIT"),
    ],
    description:
      "Bình nước giữ nhiệt 500ml, in logo UIT, phù hợp mang theo mọi nơi.",
    featured: true,
    colors: [
      { name: "Blue", value: "#D4F0FF", image: productImage("d4f0ff", "BOTTLE") },
      { name: "White", value: "#FFFFFF", image: productImage("ffffff", "BOTTLE") },
    ],
    sizeLabel: "Dung tích",
    sizeOptions: ["500ml"],
    stock: 24,
    material: "Thép không gỉ",
    detailSections: [
      {
        title: "Công năng",
        content:
          "Thiết kế gọn, dễ bỏ balo và đủ dùng cho một buổi học hoặc một buổi họp câu lạc bộ.",
      },
      {
        title: "Lưu ý sử dụng",
        content:
          "Không dùng trong lò vi sóng. Nên rửa bằng tay để lớp in bên ngoài giữ màu lâu hơn.",
      },
    ],
  },
  {
    id: "4",
    name: "Nón Lưỡi Trai UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Thời trang",
    price: 95000,
    image: productImage("ffe4b5", "CAP"),
    gallery: [
      productImage("ffe4b5", "CAP"),
      productImage("f8e697", "SIDE"),
      productImage("d4f0ff", "UIT"),
    ],
    description:
      "Mũ lưỡi trai phối màu xanh-trắng thể hiện tinh thần UIT.",
    colors: [
      { name: "Sand", value: "#FFE4B5", image: productImage("ffe4b5", "CAP") },
      { name: "Navy", value: "#1A3A4A", image: productImage("1a3a4a", "CAP") },
    ],
    sizeLabel: "Kích cỡ",
    sizeOptions: ["Vòng đầu 56-60cm"],
    stock: 31,
    material: "Kaki cotton",
    detailSections: [
      {
        title: "Chi tiết",
        content:
          "Nón có dây chỉnh phía sau, logo UIT thêu nổi ở mặt trước và lưỡi nón cứng vừa phải.",
      },
      {
        title: "Phù hợp",
        content:
          "Dùng tốt cho hoạt động ngoài trời, sự kiện trường hoặc outfit đi học thường ngày.",
      },
    ],
  },
  {
    id: "5",
    name: "Tote Bag Canvas",
    orgName: "UIT Media & Design",
    category: "Túi xách",
    price: 75000,
    image: productImage("e0ffe4", "TOTE"),
    gallery: [
      productImage("e0ffe4", "TOTE"),
      productImage("ffffff", "CANVAS"),
      productImage("f8e697", "DETAIL"),
    ],
    description:
      "Túi tote canvas thân thiện môi trường, thiết kế tối giản bởi đội ngũ UMD.",
    colors: [
      { name: "Natural", value: "#EDE6D6", image: productImage("ede6d6", "TOTE") },
      { name: "Mint", value: "#E0FFE4", image: productImage("e0ffe4", "TOTE") },
    ],
    sizeLabel: "Kích thước",
    sizeOptions: ["38 x 42cm"],
    stock: 27,
    material: "Canvas dày",
    detailSections: [
      {
        title: "Sức chứa",
        content:
          "Đựng vừa laptop mỏng, sách vở và phụ kiện cá nhân cho một ngày học ở trường.",
      },
      {
        title: "Thiết kế",
        content:
          "Đường may chắc, quai đeo dài vừa vai và phần in tối giản dễ phối đồ.",
      },
    ],
  },
  {
    id: "6",
    name: "Áo Thun CS2024",
    orgName: "CLB Khoa học Máy tính",
    category: "Thời trang",
    price: 220000,
    image: productImage("ffe0f0", "TSHIRT"),
    gallery: [
      productImage("ffe0f0", "TSHIRT"),
      productImage("ffffff", "FRONT"),
      productImage("ccf5ff", "BACK"),
    ],
    description:
      "Áo thun kỉ niệm CS2024, in hình vi mạch điện nghệ thuật ở mặt lưng.",
    colors: [
      { name: "Pink", value: "#FFE0F0", image: productImage("ffe0f0", "TSHIRT") },
      { name: "White", value: "#FFFFFF", image: productImage("ffffff", "TSHIRT") },
    ],
    sizeLabel: "Kích cỡ",
    sizeOptions: ["S", "M", "L", "XL"],
    stock: 14,
    material: "Cotton compact",
    detailSections: [
      {
        title: "Ý tưởng",
        content:
          "Họa tiết vi mạch lấy cảm hứng từ các môn nền tảng của ngành khoa học máy tính.",
      },
      {
        title: "Form áo",
        content:
          "Form regular, cổ tròn, dễ mặc trong lớp học, workshop hoặc team building.",
      },
    ],
  },
  {
    id: "7",
    name: "Keychain Rồng Vàng",
    orgName: "Google Developer Groups UIT",
    category: "Phụ kiện",
    price: 35000,
    image: productImage("fff7cc", "KEY"),
    gallery: [
      productImage("fff7cc", "KEY"),
      productImage("f8e697", "DRAGON"),
      productImage("92fbff", "GDG"),
    ],
    description:
      "Móc khóa hình rồng vàng enamel pin giới hạn, phát tại sự kiện GDG DevFest.",
    colors: [{ name: "Gold", value: "#F8D987", image: productImage("fff7cc", "KEY") }],
    sizeLabel: "Kích thước",
    sizeOptions: ["4 x 6cm"],
    stock: 36,
    material: "Hợp kim phủ enamel",
    detailSections: [
      {
        title: "Phiên bản",
        content:
          "Thiết kế nhỏ gọn, màu sắc nổi bật và phù hợp gắn balo, túi tote hoặc chùm chìa khóa.",
      },
      {
        title: "Hoàn thiện",
        content:
          "Bề mặt phủ bóng, viền kim loại chắc tay và vòng móc dễ tháo lắp.",
      },
    ],
  },
  {
    id: "8",
    name: "Notebook UIT 2025",
    orgName: "Hội Sinh viên UIT",
    category: "Dụng cụ",
    price: 55000,
    image: productImage("e8e8ff", "NOTE"),
    gallery: [
      productImage("e8e8ff", "NOTE"),
      productImage("ffffff", "200 PAGES"),
      productImage("d4f0ff", "UIT"),
    ],
    description:
      "Sổ tay bìa cứng 200 trang, bìa in logo UIT, giấy 100gsm không lem mực.",
    colors: [
      { name: "Lavender", value: "#E8E8FF", image: productImage("e8e8ff", "NOTE") },
      { name: "Sky", value: "#D4F0FF", image: productImage("d4f0ff", "NOTE") },
    ],
    sizeLabel: "Khổ giấy",
    sizeOptions: ["A5 - 200 trang"],
    stock: 62,
    material: "Giấy 100gsm",
    detailSections: [
      {
        title: "Bên trong",
        content:
          "Ruột sổ kẻ dòng nhẹ, phù hợp ghi chú bài học, meeting note hoặc sketch ý tưởng nhanh.",
      },
      {
        title: "Bìa sổ",
        content:
          "Bìa cứng bo góc nhẹ, hạn chế cong mép khi để trong balo.",
      },
    ],
  },
  {
    id: "9",
    name: "Pin Badge Logo UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Phụ kiện",
    image: productImage("ffd6cc", "BADGE"),
    gallery: [
      productImage("ffd6cc", "BADGE"),
      productImage("ffffff", "PIN"),
      productImage("f8e697", "UIT"),
    ],
    description:
      "Huy hiệu ghim áo logo UIT phát miễn phí tại Lễ chào tân sinh viên 2025.",
    colors: [{ name: "Coral", value: "#FFD6CC", image: productImage("ffd6cc", "BADGE") }],
    sizeLabel: "Kích thước",
    sizeOptions: ["Đường kính 3.2cm"],
    stock: 80,
    material: "Kim loại phủ màu",
    detailSections: [
      {
        title: "Thông tin",
        content:
          "Badge nhỏ, nhẹ, dễ ghim lên áo khoác, balo hoặc dây đeo thẻ trong các sự kiện UIT.",
      },
      {
        title: "Cách nhận",
        content:
          "Sản phẩm thuộc nhóm quà tặng sự kiện, số lượng có thể thay đổi theo từng chương trình.",
      },
    ],
  },
  {
    id: "10",
    name: "Cốc Sứ GDG",
    orgName: "Google Developer Groups UIT",
    category: "Dụng cụ",
    price: 85000,
    image: productImage("ccf5ff", "MUG"),
    gallery: [
      productImage("ccf5ff", "MUG"),
      productImage("ffffff", "350ML"),
      productImage("92fbff", "GDG"),
    ],
    description:
      "Cốc sứ cao cấp 350ml, thiết kế gradient xanh đặc trưng của GDG.",
    colors: [
      { name: "Blue", value: "#CCF5FF", image: productImage("ccf5ff", "MUG") },
      { name: "White", value: "#FFFFFF", image: productImage("ffffff", "MUG") },
    ],
    sizeLabel: "Dung tích",
    sizeOptions: ["350ml"],
    stock: 22,
    material: "Sứ tráng men",
    detailSections: [
      {
        title: "Trải nghiệm",
        content:
          "Cầm chắc tay, phù hợp dùng ở bàn học, văn phòng câu lạc bộ hoặc làm quà tặng workshop.",
      },
      {
        title: "Bảo quản",
        content:
          "Rửa nhẹ bằng miếng mềm để giữ bề mặt men và phần in luôn sáng.",
      },
    ],
  },
  {
    id: "11",
    name: "Dây Đeo Thẻ UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Phụ kiện",
    image: productImage("f0e4ff", "STRAP"),
    gallery: [
      productImage("f0e4ff", "STRAP"),
      productImage("ffffff", "LANYARD"),
      productImage("d4f0ff", "UIT"),
    ],
    description:
      "Dây đeo thẻ sinh viên phát tại tuần sinh hoạt công dân đầu năm học.",
    colors: [
      { name: "Lavender", value: "#F0E4FF", image: productImage("f0e4ff", "STRAP") },
      { name: "Blue", value: "#D4F0FF", image: productImage("d4f0ff", "STRAP") },
    ],
    sizeLabel: "Chiều dài",
    sizeOptions: ["45cm"],
    stock: 100,
    material: "Polyester mềm",
    detailSections: [
      {
        title: "Sử dụng",
        content:
          "Dây đeo nhẹ, bản vừa, phù hợp mang thẻ sinh viên, thẻ sự kiện hoặc chìa khóa.",
      },
      {
        title: "Chi tiết",
        content:
          "Móc kim loại chắc, mặt dây in nhận diện UIT rõ ràng và dễ phối với trang phục hằng ngày.",
      },
    ],
  },
  {
    id: "12",
    name: "Áo Khoác Dù UIT",
    orgName: "Hội Sinh viên UIT",
    category: "Thời trang",
    price: 450000,
    image: productImage("dcffe8", "JACKET"),
    gallery: [
      productImage("dcffe8", "JACKET"),
      productImage("ffffff", "FRONT"),
      productImage("d4f0ff", "BACK"),
    ],
    description:
      "Áo khoác dù 2 lớp chống gió, thêu logo UIT tại ngực trái, có túi kéo khóa.",
    colors: [
      { name: "Mint", value: "#DCFFE8", image: productImage("dcffe8", "JACKET") },
      { name: "Navy", value: "#131B2E", image: productImage("131b2e", "JACKET") },
    ],
    sizeLabel: "Kích cỡ",
    sizeOptions: ["S", "M", "L", "XL"],
    stock: 12,
    material: "Vải dù 2 lớp",
    detailSections: [
      {
        title: "Tính năng",
        content:
          "Lớp ngoài cản gió nhẹ, lớp trong thoáng, phù hợp những ngày mưa nhỏ hoặc di chuyển trong khuôn viên.",
      },
      {
        title: "Chi tiết may",
        content:
          "Túi kéo khóa hai bên, cổ cao vừa phải và logo thêu tinh tế ở ngực trái.",
      },
    ],
  },
];

export const FEATURED_PRODUCTS = MOCK_PRODUCTS.filter((p) => p.featured);

export function findProductById(id: string | undefined) {
  return MOCK_PRODUCTS.find((product) => product.id === id);
}
