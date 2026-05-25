import { apiClient } from "./client";
import type { ApiResponse, NotificationResponse } from "../types/shared";

export async function getOrgNotifications(params?: { page?: number; size?: number }) {
  const { data } = await apiClient.get<ApiResponse<NotificationResponse[]>>(
    "/api/v1/organizer/notifications",
    { params },
  );
  return data;
}

export async function getOrgUnreadCount() {
  const { data } = await apiClient.get<ApiResponse<{ unreadCount: number }>>(
    "/api/v1/organizer/notifications/unread-count",
  );
  return data;
}

export async function markOrgNotificationRead(id: string) {
  const { data } = await apiClient.patch<ApiResponse<NotificationResponse>>(
    `/api/v1/organizer/notifications/${id}/read`,
  );
  return data;
}

export async function markAllOrgNotificationsRead() {
  const { data } = await apiClient.patch<ApiResponse<{ updated: number }>>(
    "/api/v1/organizer/notifications/read-all",
  );
  return data;
}
