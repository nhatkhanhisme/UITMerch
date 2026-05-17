import { apiClient } from "./client";
import type {
  ApiResponse,
  GuestOrderRequest,
  InstantOrderRequest,
  OrderResponse,
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

export async function cancelCustomerOrder(id: string) {
  const { data } = await apiClient.patch<ApiResponse<OrderResponse>>(
    `/api/v1/customer/orders/${id}/cancel`,
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
