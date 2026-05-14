import { apiClient } from "./client";
import type { ApiResponse, MerchResponse } from "../types/shared";

export type GetMerchListParams = {
  keyword?: string;
  category?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export async function getPublicMerchList(params?: GetMerchListParams) {
  const { data } = await apiClient.get<ApiResponse<MerchResponse[]>>(
    "/api/v1/public/merch",
    { params },
  );
  return data;
}

export async function getPopularMerch() {
  const { data } = await apiClient.get<ApiResponse<MerchResponse[]>>(
    "/api/v1/public/merch/popular",
  );
  return data;
}

export async function getPublicMerchDetail(id: string) {
  const { data } = await apiClient.get<ApiResponse<MerchResponse>>(
    `/api/v1/public/merch/${id}`,
  );
  return data;
}
