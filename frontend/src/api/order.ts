import { apiClient } from "./client";
import type {
  ApiResponse,
  CancelOrderRequest,
  GuestOrderRequest,
  InstantOrderRequest,
  OrderResponse,
  PickupScheduleRequest,
  PickupScheduleResponse,
  UpdateOrderStatusRequest,
} from "../types/shared";

export async function createGuestCheckoutOrder(request: GuestOrderRequest) {
  const { data } = await apiClient.post<ApiResponse<OrderResponse[]>>(
    "/api/v1/public/orders",
    request,
  );
  return data;
}

// ─── Customer Orders ───────────────────────────────────────────────────────────

export type GetCustomerOrdersParams = {
  status?: string;
  page?: number;
  size?: number;
};

export async function getCustomerOrders(params?: GetCustomerOrdersParams) {
  const { data } = await apiClient.get<ApiResponse<OrderResponse[]>>(
    "/api/v1/customer/orders",
    { params },
  );
  return data;
}

export async function getCustomerOrder(id: string) {
  const { data } = await apiClient.get<ApiResponse<OrderResponse>>(
    `/api/v1/customer/orders/${id}`,
  );
  return data;
}

export async function createInstantOrder(request: InstantOrderRequest) {
  const { data } = await apiClient.post<ApiResponse<OrderResponse>>(
    "/api/v1/customer/orders/instant",
    request,
  );
  return data;
}

export async function cancelCustomerOrder(id: string, request: CancelOrderRequest) {
  const { data } = await apiClient.patch<ApiResponse<OrderResponse>>(
    `/api/v1/customer/orders/${id}/cancel`,
    request,
  );
  return data;
}

// ─── Organizer Orders ──────────────────────────────────────────────────────────

export type GetOrgOrdersParams = {
  status?: string;
  page?: number;
  size?: number;
};

export async function getOrgOrders(orgId: string, params?: GetOrgOrdersParams) {
  const { data } = await apiClient.get<ApiResponse<OrderResponse[]>>(
    `/api/v1/organizations/${orgId}/orders`,
    { params },
  );
  return data;
}

export async function getOrgOrder(orgId: string, orderId: string) {
  const { data } = await apiClient.get<ApiResponse<OrderResponse>>(
    `/api/v1/organizations/${orgId}/orders/${orderId}`,
  );
  return data;
}

export async function updateOrgOrderStatus(
  orgId: string,
  orderId: string,
  request: UpdateOrderStatusRequest,
) {
  const { data } = await apiClient.patch<ApiResponse<OrderResponse>>(
    `/api/v1/organizations/${orgId}/orders/${orderId}/status`,
    request,
  );
  return data;
}

export async function cancelOrgOrder(
  orgId: string,
  orderId: string,
  request: CancelOrderRequest,
) {
  const { data } = await apiClient.patch<ApiResponse<OrderResponse>>(
    `/api/v1/organizations/${orgId}/orders/${orderId}/cancel`,
    request,
  );
  return data;
}

export async function checkInOrder(orgId: string, orderId: string) {
  const { data } = await apiClient.patch<ApiResponse<OrderResponse>>(
    `/api/v1/organizations/${orgId}/orders/${orderId}/checkin`,
  );
  return data;
}

export async function createPickupSchedule(orgId: string, request: PickupScheduleRequest) {
  const { data } = await apiClient.post<ApiResponse<PickupScheduleResponse>>(
    `/api/v1/organizations/${orgId}/pickup-schedules`,
    request,
  );
  return data;
}

export async function getPickupSchedules(orgId: string, params?: { page?: number; size?: number }) {
  const { data } = await apiClient.get<ApiResponse<PickupScheduleResponse[]>>(
    `/api/v1/organizations/${orgId}/pickup-schedules`,
    { params },
  );
  return data;
}

export async function getPickupScheduleOrders(orgId: string, scheduleId: string) {
  const { data } = await apiClient.get<ApiResponse<OrderResponse[]>>(
    `/api/v1/organizations/${orgId}/pickup-schedules/${scheduleId}/orders`,
  );
  return data;
}
