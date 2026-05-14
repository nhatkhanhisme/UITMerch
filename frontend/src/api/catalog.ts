import { appConfig } from "../config/env";
import { MOCK_PRODUCTS, type MockProduct } from "../mocks/merchData";
import {
  MOCK_ORGANIZATIONS,
  type MockOrganization,
} from "../mocks/orgData";
import { apiClient } from "./client";
import type { ApiResponse } from "./auth";

type PaginationMeta = {
  page?: number;
  pageSize?: number;
  totalElements?: number;
  totalPages?: number;
  hasNext?: boolean;
  hasPrevious?: boolean;
};

type BackendMerch = {
  id: string;
  orgId: string;
  name: string;
  description?: string | null;
  price?: number | string | null;
  stock: number;
  imageUrl?: string | null;
  status?: string | null;
  categoryId?: string | null;
  categorySlug?: string | null;
  categoryName?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BackendOrganization = {
  id: string;
  ownerId?: string | null;
  name: string;
  description?: string | null;
  logoUrl?: string | null;
  coverUrl?: string | null;
  status?: string | null;
  totalMerch?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type CatalogList<T> = {
  items: T[];
  meta?: PaginationMeta;
};

export type CatalogError = Error & {
  isCatalogError: true;
};

const placeholderImage = (text: string) =>
  `https://placehold.co/900x900/e9feff/1a3a4a?font=montserrat&text=${encodeURIComponent(text)}`;

const normalizePrice = (price: BackendMerch["price"]) => {
  if (price === undefined || price === null) {
    return undefined;
  }

  const value = typeof price === "number" ? price : Number(price);
  return Number.isFinite(value) ? value : undefined;
};

const shortNameFromName = (name: string) => {
  const words = name.trim().split(/\s+/).filter(Boolean);
  const initials = words
    .slice(0, 3)
    .map((word) => word[0])
    .join("")
    .toUpperCase();

  return initials || name.slice(0, 3).toUpperCase();
};

const toCatalogError = (error: unknown, fallbackMessage: string): CatalogError => {
  const message =
    error instanceof Error && error.message.trim().length > 0
      ? error.message
      : fallbackMessage;

  return Object.assign(new Error(message), { isCatalogError: true as const });
};

export function mapBackendMerchToProduct(
  merch: BackendMerch,
  orgName = "UITMerch",
): MockProduct {
  const image = merch.imageUrl || placeholderImage(merch.name);
  const category = merch.categoryName || merch.categorySlug || "Vật phẩm";

  return {
    id: merch.id,
    name: merch.name,
    orgName,
    category,
    price: normalizePrice(merch.price),
    image,
    gallery: [image],
    description: merch.description || "Thông tin sản phẩm đang được cập nhật.",
    featured: false,
    colors: [{ name: "Default", value: "#92FBFF", image }],
    sizeLabel: "Tùy chọn",
    sizeOptions: ["Tiêu chuẩn"],
    stock: merch.stock,
    material: undefined,
    detailSections: [
      {
        title: "Thông tin sản phẩm",
        content: merch.description || "Thông tin sản phẩm đang được cập nhật.",
      },
    ],
  };
}

export function mapBackendOrganizationToOrganization(
  organization: BackendOrganization,
): MockOrganization {
  return {
    id: organization.id,
    name: organization.name,
    shortName: shortNameFromName(organization.name),
    category: organization.status || "Tổ chức",
    memberCount: organization.totalMerch ?? 0,
    logo:
      organization.logoUrl ||
      `https://placehold.co/400x400/e9feff/1a3a4a?font=montserrat&text=${encodeURIComponent(
        shortNameFromName(organization.name),
      )}`,
    description:
      organization.description || "Thông tin tổ chức đang được cập nhật.",
  };
}

export async function getCatalogProducts(): Promise<CatalogList<MockProduct>> {
  if (appConfig.useMock) {
    return { items: MOCK_PRODUCTS };
  }

  try {
    const { data } = await apiClient.get<
      ApiResponse<BackendMerch[]> & { meta?: PaginationMeta }
    >("/api/v1/public/merch", {
      params: { page: 0, size: 100 },
    });

    return {
      items: (data.data ?? []).map((item) => mapBackendMerchToProduct(item)),
      meta: data.meta,
    };
  } catch (error) {
    throw toCatalogError(error, "Không thể tải danh sách vật phẩm.");
  }
}

export async function getPopularProducts(): Promise<MockProduct[]> {
  if (appConfig.useMock) {
    return MOCK_PRODUCTS.filter((product) => product.featured);
  }

  try {
    const { data } = await apiClient.get<ApiResponse<BackendMerch[]>>(
      "/api/v1/public/merch/popular",
    );

    return (data.data ?? []).map((item) => ({
      ...mapBackendMerchToProduct(item),
      featured: true,
    }));
  } catch (error) {
    throw toCatalogError(error, "Không thể tải vật phẩm nổi bật.");
  }
}

export async function getCatalogProductById(
  id: string | undefined,
): Promise<MockProduct | null> {
  if (!id) {
    return null;
  }

  if (appConfig.useMock) {
    return MOCK_PRODUCTS.find((product) => product.id === id) ?? null;
  }

  try {
    const { data } = await apiClient.get<ApiResponse<BackendMerch>>(
      `/api/v1/public/merch/${id}`,
    );

    return data.data ? mapBackendMerchToProduct(data.data) : null;
  } catch (error) {
    throw toCatalogError(error, "Không thể tải chi tiết vật phẩm.");
  }
}

export async function getCatalogOrganizations(): Promise<
  CatalogList<MockOrganization>
> {
  if (appConfig.useMock) {
    return { items: MOCK_ORGANIZATIONS };
  }

  try {
    const { data } = await apiClient.get<
      ApiResponse<BackendOrganization[]> & { meta?: PaginationMeta }
    >("/api/v1/public/organizations", {
      params: { page: 0, size: 100 },
    });

    return {
      items: (data.data ?? []).map(mapBackendOrganizationToOrganization),
      meta: data.meta,
    };
  } catch (error) {
    throw toCatalogError(error, "Không thể tải danh sách tổ chức.");
  }
}

export async function getCatalogOrganizationById(
  id: string | undefined,
): Promise<MockOrganization | null> {
  if (!id) {
    return null;
  }

  if (appConfig.useMock) {
    return (
      MOCK_ORGANIZATIONS.find((organization) => organization.id === id) ?? null
    );
  }

  try {
    const { data } = await apiClient.get<ApiResponse<BackendOrganization>>(
      `/api/v1/public/organizations/${id}`,
    );

    return data.data ? mapBackendOrganizationToOrganization(data.data) : null;
  } catch (error) {
    throw toCatalogError(error, "Không thể tải chi tiết tổ chức.");
  }
}

export async function getCatalogOrganizationProducts(
  organization: MockOrganization,
): Promise<CatalogList<MockProduct>> {
  if (appConfig.useMock) {
    return {
      items: MOCK_PRODUCTS.filter(
        (product) => product.orgName === organization.name,
      ),
    };
  }

  try {
    const { data } = await apiClient.get<
      ApiResponse<BackendMerch[]> & { meta?: PaginationMeta }
    >(`/api/v1/public/organizations/${organization.id}/merch`, {
      params: { page: 0, size: 100 },
    });

    return {
      items: (data.data ?? []).map((item) =>
        mapBackendMerchToProduct(item, organization.name),
      ),
      meta: data.meta,
    };
  } catch (error) {
    throw toCatalogError(error, "Không thể tải vật phẩm của tổ chức.");
  }
}

