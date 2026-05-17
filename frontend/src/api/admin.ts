import { apiClient } from "./client";
import type { ApiResponse, OrderResponse, OrganizationResponse, UserSummaryResponse } from "../types/shared";

export type ListUsersParams = {
  role?: string;
  page?: number;
  size?: number;
};

export type ListOrgsParams = {
  status?: string;
  page?: number;
  size?: number;
};

export type ListOrdersParams = {
  status?: string;
  page?: number;
  size?: number;
};

export async function adminListUsers(params?: ListUsersParams) {
  const { data } = await apiClient.get<ApiResponse<UserSummaryResponse[]>>(
    "/api/v1/admin/users",
    { params },
  );
  return data;
}

export async function adminUpdateUserRole(userId: string, role: string) {
  const { data } = await apiClient.patch<ApiResponse<UserSummaryResponse>>(
    `/api/v1/admin/users/${userId}/role`,
    { role },
  );
  return data;
}

export async function adminSetUserActive(userId: string, active: boolean) {
  const { data } = await apiClient.patch<ApiResponse<UserSummaryResponse>>(
    `/api/v1/admin/users/${userId}/active`,
    null,
    { params: { active } },
  );
  return data;
}

export async function adminListOrganizations(params?: ListOrgsParams) {
  const { data } = await apiClient.get<ApiResponse<OrganizationResponse[]>>(
    "/api/v1/admin/organizations",
    { params },
  );
  return data;
}

export async function adminUpdateOrgStatus(orgId: string, status: string) {
  const { data } = await apiClient.patch<ApiResponse<OrganizationResponse>>(
    `/api/v1/admin/organizations/${orgId}/status`,
    { status },
  );
  return data;
}

export async function adminListOrders(params?: ListOrdersParams) {
  const { data } = await apiClient.get<ApiResponse<OrderResponse[]>>(
    "/api/v1/admin/orders",
    { params },
  );
  return data;
}
