import { apiClient } from "./client";
import { useAuthStore } from "../stores/authStore";
const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ALLOWED_MIME_TYPES = new Set(["image/jpeg", "image/png", "image/gif", "image/webp"]);
const uploadImage = async (file: File, path: string) => {
  if (!ALLOWED_MIME_TYPES.has(file.type)) throw new Error("Vui lòng chọn ảnh JPG, PNG, GIF hoặc WebP. SVG không được hỗ trợ.");
  if (!file.size || file.size > MAX_FILE_SIZE) throw new Error("Ảnh phải nhỏ hơn hoặc bằng 10MB.");
  const body = new FormData(); body.append("file", file);
  const response = await apiClient.post(path, body);
  const url = response.data?.data?.fileUrl;
  if (typeof url !== "string" || !url) throw new Error("Không thể tải ảnh lên. Vui lòng thử lại.");
  return url;
};
export async function uploadAvatarImage(file: File, userId: string) {
  if (useAuthStore.getState().user?.id !== userId) throw new Error("Bạn chỉ có thể tải ảnh cho tài khoản của mình.");
  return uploadImage(file, "/api/v1/uploads/avatar");
}
export async function uploadOrganizerImage(file: File, organizationId: string, variant: "logo" | "cover") {
  return uploadImage(file, `/api/v1/uploads/organizations/${encodeURIComponent(organizationId)}/${variant}`);
}
export async function uploadMerchImage(file: File, orgId: string) {
  return uploadImage(file, `/api/v1/uploads/organizations/${encodeURIComponent(orgId)}/merch`);
}
export async function uploadEventImage(file: File, orgId: string) {
  return uploadImage(file, `/api/v1/uploads/organizations/${encodeURIComponent(orgId)}/events`);
}
