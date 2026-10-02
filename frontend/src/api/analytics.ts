import { apiClient } from "./client";
import type { ApiResponse } from "../types/shared";
import type { Analytics } from "../types/features";
export async function getAnalytics(orgId: string, from?: string, to?: string) {
  return (
    await apiClient.get<ApiResponse<Analytics>>(
      `/api/v1/organizations/${orgId}/analytics`,
      { params: { from, to } },
    )
  ).data.data;
}
