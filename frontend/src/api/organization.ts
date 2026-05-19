import { apiClient } from "./client";
import type {
  ApiResponse,
  CreateOrganizationRequest,
  EventResponse,
  MerchResponse,
  OrganizationResponse,
} from "../types/shared";

export type PaginationParams = {
  page?: number;
  size?: number;
  sort?: string;
};

export async function getPublicOrganizations(params?: PaginationParams) {
  const { data } = await apiClient.get<ApiResponse<OrganizationResponse[]>>(
    "/api/v1/public/organizations",
    { params },
  );
  return data;
}

export async function getPublicOrganizationDetail(id: string) {
  const { data } = await apiClient.get<ApiResponse<OrganizationResponse>>(
    `/api/v1/public/organizations/${id}`,
  );
  return data;
}

export async function getPublicOrgMerch(id: string, params?: PaginationParams) {
  const { data } = await apiClient.get<ApiResponse<MerchResponse[]>>(
    `/api/v1/public/organizations/${id}/merch`,
    { params },
  );
  return data;
}

export async function getPublicOrgEvents(id: string, params?: PaginationParams) {
  const { data } = await apiClient.get<ApiResponse<EventResponse[]>>(
    `/api/v1/public/organizations/${id}/events`,
    { params },
  );
  return data;
}

// ─── Organizer Organization Management ────────────────────────────────────────

export async function getOwnOrganizations(params?: PaginationParams) {
  const { data } = await apiClient.get<ApiResponse<OrganizationResponse[]>>(
    "/api/v1/organizations/mine",
    { params },
  );
  return data;
}

export async function createOrganization(request: CreateOrganizationRequest) {
  const { data } = await apiClient.post<ApiResponse<OrganizationResponse>>(
    "/api/v1/organizations",
    request,
  );
  return data;
}

export async function updateOrganization(
  id: string,
  request: Partial<CreateOrganizationRequest>,
) {
  const { data } = await apiClient.patch<ApiResponse<OrganizationResponse>>(
    `/api/v1/organizations/${id}`,
    request,
  );
  return data;
}
