import { apiClient } from "./client";
import type {
  ApiResponse,
  CreateEventRequest,
  EventResponse,
  UpdateEventRequest,
} from "../types/shared";

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

// ─── Organizer Event Management ────────────────────────────────────────────────

export type GetOwnEventsParams = {
  page?: number;
  size?: number;
};

export async function getOwnEvents(orgId: string, params?: GetOwnEventsParams) {
  const { data } = await apiClient.get<ApiResponse<EventResponse[]>>(
    `/api/v1/organizations/${orgId}/events`,
    { params },
  );
  return data;
}

export async function createEvent(orgId: string, request: CreateEventRequest) {
  const { data } = await apiClient.post<ApiResponse<EventResponse>>(
    `/api/v1/organizations/${orgId}/events`,
    request,
  );
  return data;
}

export async function updateEvent(
  orgId: string,
  eventId: string,
  request: UpdateEventRequest,
) {
  const { data } = await apiClient.patch<ApiResponse<EventResponse>>(
    `/api/v1/organizations/${orgId}/events/${eventId}`,
    request,
  );
  return data;
}

export async function attachMerchToEvent(
  orgId: string,
  eventId: string,
  merchId: string,
) {
  const { data } = await apiClient.post<ApiResponse<EventResponse>>(
    `/api/v1/organizations/${orgId}/events/${eventId}/merch`,
    { merchId },
  );
  return data;
}

export async function deleteEvent(orgId: string, eventId: string) {
  await apiClient.delete(`/api/v1/organizations/${orgId}/events/${eventId}`);
}

export async function detachMerchFromEvent(
  orgId: string,
  eventId: string,
  merchId: string,
) {
  await apiClient.delete(
    `/api/v1/organizations/${orgId}/events/${eventId}/merch/${merchId}`,
  );
}
