import { apiClient } from "./client";
import type { ApiResponse, WishlistResponse } from "../types/shared";

export async function getWishlist() {
  const { data } = await apiClient.get<ApiResponse<WishlistResponse>>(
    "/api/v1/customer/wishlist",
  );
  return data;
}

export async function addToWishlist(merchId: string) {
  const { data } = await apiClient.post<ApiResponse<WishlistResponse>>(
    `/api/v1/customer/wishlist/${merchId}`,
  );
  return data;
}

export async function removeFromWishlist(merchId: string) {
  await apiClient.delete(`/api/v1/customer/wishlist/${merchId}`);
}
