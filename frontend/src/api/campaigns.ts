import { apiClient, publicClient } from "./client";
import type { ApiResponse, OrderResponse } from "../types/shared";
import type {
  Campaign,
  SpringPage,
  Reservation,
  ReserveRequest,
  PurchaseContext,
  CampaignOrderContext,
} from "../types/features";
export type CreateCampaignRequest = {
  title: string;
  description?: string;
  minimumQuantity: number;
  deadline: string;
  variants: { merchId: string; label: string }[];
};
export async function listCampaigns(page = 0, orgId?: string) {
  const client = orgId ? apiClient : publicClient;
  const path = orgId
    ? `/api/v1/organizations/${orgId}/campaigns`
    : "/api/v1/public/campaigns";
  return (
    await client.get<ApiResponse<SpringPage<Campaign>>>(path, {
      params: { page, size: 20 },
    })
  ).data.data;
}
export async function getCampaign(id: string, orgId?: string) {
  const client = orgId ? apiClient : publicClient;
  const path = orgId
    ? `/api/v1/organizations/${orgId}/campaigns/${id}`
    : `/api/v1/public/campaigns/${id}`;
  return (await client.get<ApiResponse<Campaign>>(path)).data.data;
}
export async function createCampaign(
  orgId: string,
  body: CreateCampaignRequest,
) {
  return (
    await apiClient.post<ApiResponse<Campaign>>(
      `/api/v1/organizations/${orgId}/campaigns`,
      body,
    )
  ).data.data;
}
export async function cancelCampaign(orgId: string, id: string) {
  return (
    await apiClient.post<ApiResponse<Campaign>>(
      `/api/v1/organizations/${orgId}/campaigns/${id}/cancel`,
    )
  ).data.data;
}
export async function reserveCampaign(id: string, body: ReserveRequest) {
  return (
    await apiClient.post<
      ApiResponse<{ reservation: Reservation; order: OrderResponse }>
    >(`/api/v1/customer/campaigns/${id}/reservations`, body)
  ).data.data;
}
export async function listReservations(page = 0) {
  return (
    await apiClient.get<ApiResponse<SpringPage<Reservation>>>(
      "/api/v1/customer/campaign-reservations",
      { params: { page, size: 20 } },
    )
  ).data.data;
}
export async function getPurchaseContext(merchId: string) {
  return (
    await apiClient.get<ApiResponse<PurchaseContext>>(
      `/api/v1/public/merch/${merchId}/purchase-context`,
    )
  ).data.data;
}
export async function getCampaignOrderContext(orderId: string, orgId?: string) {
  const path = orgId
    ? `/api/v1/organizations/${orgId}/orders/${orderId}/campaign-context`
    : `/api/v1/customer/orders/${orderId}/campaign-context`;
  return (await apiClient.get<ApiResponse<CampaignOrderContext>>(path)).data
    .data;
}
