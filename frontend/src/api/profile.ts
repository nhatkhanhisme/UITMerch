import { apiClient } from "./client";
import type { ApiResponse } from "./auth";
import type { CustomerProfile, OrganizerProfile } from "../types/profile";

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
