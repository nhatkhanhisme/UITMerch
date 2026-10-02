import axios from "axios";
import type {
  ApiResponse,
  PaginationMeta,
  NotificationResponse,
} from "../types/shared";
import type { SpringPage } from "../types/features";
export function normalizePage<T>(response: ApiResponse<T[] | SpringPage<T>>) {
  if (Array.isArray(response.data))
    return { items: response.data, meta: response.meta };
  const p = response.data;
  if (!p || !Array.isArray(p.content))
    throw new Error("Phản hồi danh sách không hợp lệ.");
  const meta: PaginationMeta = {
    page: p.number,
    pageSize: p.size,
    totalElements: p.totalElements,
    totalPages: p.totalPages,
    hasNext: !p.last,
    hasPrevious: !p.first,
  };
  return { items: p.content, meta };
}
export async function collectPages<T>(
  get: (page: number) => Promise<SpringPage<T>>,
  maxPages = 100,
) {
  const items: T[] = [];
  for (let page = 0; page < maxPages; page++) {
    const response = await get(page);
    items.push(...response.content);
    if (response.last) return items;
  }
  throw new Error("Danh sách quá lớn. Vui lòng mở trang quản lý để kiểm tra.");
}
export function featureError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    if (status === 429)
      return `Bạn thao tác quá nhanh. Thử lại sau ${error.response?.headers["retry-after"] ?? "một vài"} giây.`;
    if (status === 401)
      return "Phiên đăng nhập hết hiệu lực. Vui lòng đăng nhập lại.";
    if (status === 403) return "Bạn không có quyền thực hiện thao tác này.";
    if (status === 404)
      return "Không tìm thấy dữ liệu hoặc bạn không có quyền truy cập.";
    return (
      error.response?.data?.message ?? "Không thể kết nối. Vui lòng thử lại."
    );
  }
  return error instanceof Error
    ? error.message
    : "Không thể hoàn tất thao tác.";
}
export const money = (value: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    value,
  );
export const campusDate = (date = new Date()) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
export const dateTime = (value: string) =>
  new Date(
    value.endsWith("Z") || /[+-]\d\d:\d\d$/.test(value)
      ? value
      : value + "+07:00",
  ).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });

export type WireNotification = Omit<NotificationResponse, "isRead"> & {
  isRead?: boolean;
  read?: boolean;
};
export function normalizeNotification(
  notification: WireNotification,
): NotificationResponse {
  return {
    ...notification,
    isRead: notification.isRead ?? notification.read ?? false,
  };
}

export const percentage = (value: number) => new Intl.NumberFormat("vi-VN", {style:"percent",maximumFractionDigits:2}).format(value);
