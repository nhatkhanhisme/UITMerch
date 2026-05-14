import { apiClient } from "./client";
import type { ApiResponse, EventResponse } from "../types/shared";
import type { PaginationParams } from "./organization";

export async function getPublicEvents(params?: PaginationParams) {
  const { data } = await apiClient.get<ApiResponse<EventResponse[]>>(
    "/api/v1/public/events",
    { params },
  );
  return data;
}

export async function getPublicEventDetail(id: string) {
  const { data } = await apiClient.get<ApiResponse<EventResponse>>(
    `/api/v1/public/events/${id}`,
  );
  return data;
}
