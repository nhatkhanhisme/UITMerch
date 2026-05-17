import { apiClient } from "./client";
import type {
  AddCartItemRequest,
  ApiResponse,
  CartResponse,
  CheckoutRequest,
  OrderResponse,
  UpdateCartItemRequest,
} from "../types/shared";

export async function getCart() {
  const { data } = await apiClient.get<ApiResponse<CartResponse>>(
    "/api/v1/customer/cart",
  );
  return data;
}

export async function addCartItem(request: AddCartItemRequest) {
  const { data } = await apiClient.post<ApiResponse<CartResponse>>(
    "/api/v1/customer/cart/items",
    request,
  );
  return data;
}

export async function updateCartItem(itemId: string, request: UpdateCartItemRequest) {
  const { data } = await apiClient.patch<ApiResponse<CartResponse>>(
    `/api/v1/customer/cart/items/${itemId}`,
    request,
  );
  return data;
}

export async function removeCartItem(itemId: string) {
  await apiClient.delete(`/api/v1/customer/cart/items/${itemId}`);
}

export async function checkoutCart(request?: CheckoutRequest) {
  const { data } = await apiClient.post<ApiResponse<OrderResponse[]>>(
    "/api/v1/customer/cart/checkout",
    request ?? {},
  );
  return data;
}
