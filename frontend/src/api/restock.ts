import { apiClient } from "./client";
import type { ApiResponse } from "../types/shared";
import type { SpringPage, RestockSubscription } from "../types/features";
const path = "/api/v1/customer/restock-subscriptions";
export async function listRestock(page = 0) {
  return (
    await apiClient.get<ApiResponse<SpringPage<RestockSubscription>>>(path, {
      params: { page, size: 20 },
    })
  ).data.data;
}
export async function subscribeRestock(merchId: string, emailEnabled: boolean) {
  return (
    await apiClient.post<ApiResponse<RestockSubscription>>(path, {
      merchId,
      emailEnabled,
    })
  ).data.data;
}
export async function unsubscribeRestock(merchId: string) {
  await apiClient.delete(`${path}/${merchId}`);
}
