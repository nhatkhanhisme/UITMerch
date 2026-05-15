import { apiClient } from "./client";
import type {
  ApiResponse,
  GuestOrderRequest,
  OrderResponse,
} from "../types/shared";

export async function createGuestCheckoutOrder(request: GuestOrderRequest) {
  const { data } = await apiClient.post<ApiResponse<OrderResponse[]>>(
    "/api/v1/public/orders",
    request,
  );
  return data;
}
