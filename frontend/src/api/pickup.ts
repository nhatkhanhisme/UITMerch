import { apiClient } from "./client";
import type { ApiResponse, OrderResponse } from "../types/shared";
import type {
  PickupCredential,
  SpringPage,
  OrderHistory,
} from "../types/features";
export async function issuePickup(orderId: string) {
  return (
    await apiClient.post<ApiResponse<PickupCredential>>(
      `/api/v1/customer/orders/${orderId}/pickup-token`,
    )
  ).data.data;
}
export async function verifyPickup(
  orgId: string,
  token: string,
  pickupScheduleId: string | null,
) {
  return (
    await apiClient.post<ApiResponse<OrderResponse>>(
      `/api/v1/organizations/${orgId}/orders/pickup/verify`,
      { token, pickupScheduleId },
    )
  ).data.data;
}
export async function completePickup(
  orgId: string,
  token: string,
  pickupScheduleId: string | null,
) {
  return (
    await apiClient.post<ApiResponse<OrderResponse>>(
      `/api/v1/organizations/${orgId}/orders/pickup/checkin`,
      { token, pickupScheduleId },
    )
  ).data.data;
}
export async function requestGuestReceipt(orderId: string, email: string) {
  await apiClient.post(`/api/v1/public/orders/${orderId}/pickup-receipt`, {
    email,
  });
}
export async function exchangeGuestReceipt(
  orderId: string,
  receiptToken: string,
) {
  return (
    await apiClient.post<ApiResponse<PickupCredential>>(
      `/api/v1/public/orders/${orderId}/pickup-token`,
      { receiptToken },
    )
  ).data.data;
}
export async function trackGuestOrder(orderId: string, trackingCode: string) {
  return (
    await apiClient.get<ApiResponse<OrderResponse>>(
      `/api/v1/public/orders/${orderId}/tracking`,
      { headers: { "X-Guest-Tracking": trackingCode } },
    )
  ).data.data;
}
export async function requestGuestTracking(orderId: string, email: string) {
  await apiClient.post(`/api/v1/public/orders/${orderId}/tracking-receipt`, { email });
}
export async function getOrderHistory(
  orderId: string,
  page = 0,
  orgId?: string,
) {
  const path = orgId
    ? `/api/v1/organizations/${orgId}/orders/${orderId}/history`
    : `/api/v1/customer/orders/${orderId}/history`;
  return (
    await apiClient.get<ApiResponse<SpringPage<OrderHistory>>>(path, {
      params: { page, size: 20 },
    })
  ).data.data;
}
