import { apiClient } from "./client";
import type { ApiResponse } from "./auth";
import type {
  CustomerProfile,
  CustomerProfileUpdate,
  OrganizerProfile,
  OrganizerProfileUpdate,
} from "../types/profile";

export async function getCustomerProfile() {
  const { data } = await apiClient.get<ApiResponse<CustomerProfile>>(
    "/api/v1/customer/profile",
  );

  return data;
}

export async function getOrganizerProfile() {
  const { data } = await apiClient.get<ApiResponse<OrganizerProfile>>(
    "/api/v1/organizations/me",
  );

  return data;
}

export async function updateCustomerProfile(request: CustomerProfileUpdate) {
  const { data } = await apiClient.patch<ApiResponse<CustomerProfile>>(
    "/api/v1/customer/profile",
    request,
  );

  return data;
}

export async function updateOrganizerProfile(request: OrganizerProfileUpdate) {
  const { data } = await apiClient.patch<ApiResponse<OrganizerProfile>>(
    "/api/v1/organizations/me",
    request,
  );

  return data;
}
