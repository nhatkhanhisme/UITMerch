import { apiClient } from "./client";
import type { ApiResponse, EventResponse } from "../types/shared";

export type GetPublicEventsParams = {
  page?: number;
  size?: number;
  sort?: string;
  status?: string;
};

export async function getPublicEvents(params?: GetPublicEventsParams) {
  const { data } = await apiClient.get<ApiResponse<EventResponse[]>>(
    "/api/v1/public/events",
    { params },
  );
  return data;
}

export async function getPublicEvent(id: string) {
  const { data } = await apiClient.get<ApiResponse<EventResponse>>(
    `/api/v1/public/events/${id}`,
  );
  return data;
}
