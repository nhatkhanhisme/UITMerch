import { apiClient } from "./client";
import type { ApiResponse, NotificationResponse } from "../types/shared";

export async function getNotifications(params?: { page?: number; size?: number }) {
  const { data } = await apiClient.get<ApiResponse<NotificationResponse[]>>(
    "/api/v1/customer/notifications",
    { params },
  );
  return data;
}

export async function getUnreadCount() {
  const { data } = await apiClient.get<ApiResponse<{ unreadCount: number }>>(
    "/api/v1/customer/notifications/unread-count",
  );
  return data;
}

export async function markNotificationRead(id: string) {
  const { data } = await apiClient.patch<ApiResponse<NotificationResponse>>(
    `/api/v1/customer/notifications/${id}/read`,
  );
  return data;
}

export async function markAllNotificationsRead() {
  const { data } = await apiClient.patch<ApiResponse<{ updated: number }>>(
    "/api/v1/customer/notifications/read-all",
  );
  return data;
}
