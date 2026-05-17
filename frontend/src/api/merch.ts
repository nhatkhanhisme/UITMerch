import { apiClient } from "./client";
import type {
  ApiResponse,
  CategoryResponse,
  CreateMerchRequest,
  MerchResponse,
  UpdateMerchRequest,
} from "../types/shared";

export type GetMerchListParams = {
  keyword?: string;
  category?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export async function getPublicMerchList(params?: GetMerchListParams) {
  const { data } = await apiClient.get<ApiResponse<MerchResponse[]>>(
    "/api/v1/public/merch",
    { params },
  );
  return data;
}

export async function getPopularMerch() {
  const { data } = await apiClient.get<ApiResponse<MerchResponse[]>>(
    "/api/v1/public/merch/popular",
  );
  return data;
}

export async function getPublicMerchDetail(id: string) {
  const { data } = await apiClient.get<ApiResponse<MerchResponse>>(
    `/api/v1/public/merch/${id}`,
  );
  return data;
}

export async function getCategories() {
  const { data } = await apiClient.get<ApiResponse<CategoryResponse[]>>(
    "/api/v1/categories",
  );
  return data;
}

// ─── Organizer Merch Management ────────────────────────────────────────────────

export type GetOwnMerchParams = {
  page?: number;
  size?: number;
};

export async function getOwnMerch(orgId: string, params?: GetOwnMerchParams) {
  const { data } = await apiClient.get<ApiResponse<MerchResponse[]>>(
    `/api/v1/organizations/${orgId}/merchs`,
    { params },
  );
  return data;
}

export async function createMerch(orgId: string, request: CreateMerchRequest) {
  const { data } = await apiClient.post<ApiResponse<MerchResponse>>(
    `/api/v1/organizations/${orgId}/merchs`,
    request,
  );
  return data;
}

export async function updateMerch(
  orgId: string,
  merchId: string,
  request: UpdateMerchRequest,
) {
  const { data } = await apiClient.patch<ApiResponse<MerchResponse>>(
    `/api/v1/organizations/${orgId}/merchs/${merchId}`,
    request,
  );
  return data;
}

export async function deleteMerch(orgId: string, merchId: string) {
  await apiClient.delete(`/api/v1/organizations/${orgId}/merchs/${merchId}`);
}
