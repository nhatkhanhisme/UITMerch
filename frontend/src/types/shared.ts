import { findProductById } from "../mocks/merchData";
import type { MockProduct } from "../mocks/merchData";
import { findOrganizationById } from "../mocks/orgData";
import type { MockOrganization } from "../mocks/orgData";

export type ButtonBaseProps = {
  variant?: "primary" | "secondary" | "outline";
  size?: "sm" | "md" | "lg";
  loading?: boolean;
};

export type InputBaseProps = {
  errorMessage?: string;
  label?: string;
  placeholder?: string;
  type?: string;
};

export type BadgeBaseProps = {
  variant?: "peach" | "gold" | "aqua";
};

export type OrderItem = {
  productId: string;
  quantity: number;
  color?: string;
  size?: string;
  price: number;
};

export type User = {
  id: string;
  email: string;
  fullName: string;
  role?: string;
};

export type Product = {
  id: string;
  name: string;
  price?: number;
  description?: string;
};

export type Category = {
  id: string;
  name: string;
  slug?: string;
};

export type Order = {
  id: string;
  userId: string;
  totalAmount: number;
  status: string;
};

export type PaginationMeta = {
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

export type ApiResponse<T> = {
  status?: number;
  success?: boolean;
  message: string;
  data: T;
  meta?: PaginationMeta;
  traceId?: string;
};

// ─── Real Backend Response Interfaces ─────────────────────────────────────────

export type CategoryResponse = {
  id: string;
  slug: string;
  name: string;
  description?: string;
  displayOrder: number;
};


export type MerchResponse = {
  id: string;
  orgId: string;
  name: string;
  description?: string;
  price?: number;
  stock: number;
  images?: string[];
  status: string;
  categoryId?: string;
  categorySlug?: string;
  categoryName?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type OrganizationResponse = {
  id: string;
  ownerId: string;
  name: string;
  description?: string;
  logoUrl?: string;
  coverUrl?: string;
  status: string;
  totalMerch?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type EventResponse = {
  id: string;
  orgId: string;
  title: string;
  description?: string;
  coverUrl?: string;
  status: string;
  startsAt?: string;
  endsAt?: string;
  createdAt?: string;
  updatedAt?: string;
  merch?: MerchResponse[];
};

export type OrderItemResponse = {
  id: string;
  orderId: string;
  merchId: string;
  merchName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  createdAt?: string;
};

export type OrderResponse = {
  id: string;
  userId?: string;
  orgId: string;
  guestName?: string;
  guestEmail?: string;
  guestPhone?: string;
  guestAddress?: string;
  totalAmount: number;
  status: string;
  paymentMethod: string;
  paymentStatus: string;
  note?: string;
  items?: OrderItemResponse[];
  createdAt?: string;
  updatedAt?: string;
};

export type GuestOrderItemRequest = {
  merchId: string;
  quantity: number;
};

export type GuestOrderRequest = {
  items: GuestOrderItemRequest[];
  guestName: string;
  guestPhone: string;
  guestAddress: string;
  guestEmail?: string;
  note?: string;
};

export type InstantOrderRequest = {
  merchId: string;
  quantity: number;
  note?: string;
};

export type CheckoutRequest = {
  note?: string;
  shippingName?: string;
  shippingPhone?: string;
  shippingAddress?: string;
};

// ─── Cart Types ────────────────────────────────────────────────────────────────

export type CartItemResponse = {
  id: string;
  cartId: string;
  merch: MerchResponse;
  quantity: number;
  subtotal: number;
  createdAt?: string;
};

export type CartResponse = {
  id: string;
  userId: string;
  status: string;
  items: CartItemResponse[];
  totalAmount: number;
  createdAt?: string;
  updatedAt?: string;
};

export type AddCartItemRequest = {
  merchId: string;
  quantity: number;
};

export type UpdateCartItemRequest = {
  quantity: number;
};

// ─── Wishlist Types ────────────────────────────────────────────────────────────

export type WishlistItemResponse = {
  id: string;
  merch: MerchResponse;
  addedAt?: string;
};

export type WishlistResponse = {
  id: string;
  items: WishlistItemResponse[];
};

// ─── Admin Types ───────────────────────────────────────────────────────────────

export type UserSummaryResponse = {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  role: string;
  isVerified: boolean;
  isActive: boolean;
  createdAt?: string;
};

// ─── Organizer Merch Request Types ────────────────────────────────────────────

export type CreateMerchRequest = {
  name: string;
  description?: string;
  price?: number;
  stock: number;
  categoryId?: string;
  imageUrls?: string[];
};

export type UpdateMerchRequest = {
  name?: string;
  description?: string;
  price?: number;
  stock?: number;
  categoryId?: string;
  imageUrls?: string[];
  status?: string;
};

// ─── Organizer Event Request Types ────────────────────────────────────────────

export type CreateEventRequest = {
  title: string;
  description?: string;
  coverUrl?: string;
  startsAt?: string;
  endsAt?: string;
};

export type UpdateEventRequest = {
  title?: string;
  description?: string;
  coverUrl?: string;
  status?: string;
  startsAt?: string;
  endsAt?: string;
};

// ─── Organizer Order Request Types ────────────────────────────────────────────

export type UpdateOrderStatusRequest = {
  status: string;
};

// ─── Organization Request Types ───────────────────────────────────────────────

export type CreateOrganizationRequest = {
  name: string;
  description?: string;
  logoUrl?: string;
  coverUrl?: string;
};

// ─── Intelligent UI Mappers (Preserving Glassmorphism & Visual WOW) ──────────

export function mapMerchToMockProduct(
  item: MerchResponse,
  orgMap?: Record<string, string>,
): MockProduct {
  const existingMock = findProductById(item.id);

  const PLACEHOLDER = "https://placehold.co/900x900/e9feff/1a3a4a?font=montserrat&text=MERCH";

  // Use the new images[] array from backend
  const backendImages = item.images?.filter(Boolean) ?? [];
  const primaryImage = backendImages[0] || existingMock?.image || PLACEHOLDER;

  // Build gallery: prefer full backend images, fall back to mock gallery, then single placeholder
  const gallery =
    backendImages.length > 0
      ? backendImages
      : existingMock?.gallery?.length
        ? existingMock.gallery
        : [primaryImage];

  const fallbackOrgName =
    orgMap?.[item.orgId] || existingMock?.orgName || "UIT Organization";

  return {
    id: item.id,
    name: item.name || existingMock?.name || "Sản phẩm UIT",
    orgName: fallbackOrgName,
    category: item.categoryName || existingMock?.category || "Vật phẩm",
    price: item.price ?? existingMock?.price ?? 0,
    image: primaryImage,
    gallery,
    description:
      item.description || existingMock?.description || "Chưa có mô tả chi tiết.",
    featured: existingMock?.featured ?? false,
    colors: existingMock?.colors?.length
      ? existingMock.colors
      : [{ name: "Mặc định", value: "#92FBFF", image: primaryImage }],
    sizeLabel: existingMock?.sizeLabel || "Phân loại",
    sizeOptions: existingMock?.sizeOptions?.length
      ? existingMock.sizeOptions
      : ["Tiêu chuẩn"],
    stock: item.stock ?? existingMock?.stock ?? 10,
    material: existingMock?.material || "Chất liệu thân thiện",
    detailSections: existingMock?.detailSections?.length
      ? existingMock.detailSections
      : [
          {
            title: "Thông tin sản phẩm",
            content:
              item.description ||
              "Vật phẩm mang tinh thần sinh viên UIT, thiết kế năng động, tiện lợi sử dụng trong học tập và các hoạt động phong trào.",
          },
        ],
  };
}

export function mapOrgToMockOrganization(
  org: OrganizationResponse,
): MockOrganization {
  const existingMock = findOrganizationById(org.id);

  const shortName =
    existingMock?.shortName ||
    org.name
      .split(" ")
      .map((w) => w[0])
      .join("")
      .slice(0, 4)
      .toUpperCase();
  const fallbackLogo =
    org.logoUrl ||
    existingMock?.logo ||
    `https://placehold.co/400x400/92fbff/1a3a4a?font=montserrat&text=${shortName}`;

  return {
    id: org.id,
    name: org.name,
    shortName: shortName,
    category: existingMock?.category || "Cộng đồng",
    memberCount: existingMock?.memberCount || 50,
    logo: fallbackLogo,
    description:
      org.description ||
      existingMock?.description ||
      "Đơn vị trực thuộc trường Đại học Công nghệ Thông tin - ĐHQG-HCM.",
  };
}
