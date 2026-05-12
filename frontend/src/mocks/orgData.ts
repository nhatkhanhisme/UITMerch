// ─── Mock organization data ──────────────────────────────────────────────────
export interface MockOrganization {
  id: string;
  name: string;
  shortName: string;
  category: string;
  memberCount: number;
  logo: string;
  description: string;
}

export const MOCK_ORGANIZATIONS: MockOrganization[] = [
  {
    id: "1",
    name: "Google Developer Groups UIT",
    shortName: "GDG UIT",
    category: "Công nghệ",
    memberCount: 320,
    logo: "https://placehold.co/400x400/92fbff/1a3a4a?font=montserrat&text=GDG",
    description: "Cộng đồng lập trình viên Google tại UIT.",
  },
  {
    id: "2",
    name: "CLB Khoa học Máy tính",
    shortName: "CS Club",
    category: "Học thuật",
    memberCount: 210,
    logo: "https://placehold.co/400x400/f8e697/1a3a4a?font=montserrat&text=CS",
    description: "Nghiên cứu & ứng dụng khoa học máy tính.",
  },
  {
    id: "3",
    name: "UIT Security Research",
    shortName: "USec",
    category: "Bảo mật",
    memberCount: 85,
    logo: "https://placehold.co/400x400/d4f0ff/1a3a4a?font=montserrat&text=SEC",
    description: "Nghiên cứu an ninh mạng và bảo mật hệ thống.",
  },
  {
    id: "4",
    name: "UIT Game Developers",
    shortName: "UIT GameDev",
    category: "Sáng tạo",
    memberCount: 140,
    logo: "https://placehold.co/400x400/ffe4b5/1a3a4a?font=montserrat&text=GAME",
    description: "Thiết kế và phát triển game indie.",
  },
  {
    id: "5",
    name: "Hội Sinh viên UIT",
    shortName: "HSV UIT",
    category: "Cộng đồng",
    memberCount: 1200,
    logo: "https://placehold.co/400x400/e0ffe4/1a3a4a?font=montserrat&text=HSV",
    description: "Tổ chức đại diện toàn thể sinh viên UIT.",
  },
  {
    id: "6",
    name: "CLB Trí tuệ Nhân tạo",
    shortName: "AI Club",
    category: "Công nghệ",
    memberCount: 175,
    logo: "https://placehold.co/400x400/ffe0f0/1a3a4a?font=montserrat&text=AI",
    description: "Nghiên cứu Machine Learning và AI ứng dụng.",
  },
  {
    id: "7",
    name: "UIT Media & Design",
    shortName: "UMD",
    category: "Sáng tạo",
    memberCount: 95,
    logo: "https://placehold.co/400x400/fff7cc/1a3a4a?font=montserrat&text=UMD",
    description: "Thiết kế đồ họa, video và truyền thông.",
  },
  {
    id: "8",
    name: "CLB Toán ứng dụng",
    shortName: "Math Club",
    category: "Học thuật",
    memberCount: 110,
    logo: "https://placehold.co/400x400/e8e8ff/1a3a4a?font=montserrat&text=MATH",
    description: "Toán học ứng dụng trong khoa học dữ liệu.",
  },
];
