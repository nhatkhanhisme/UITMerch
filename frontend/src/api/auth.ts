import axios from "axios";
import { apiClient } from "./client";
import type { AuthSession, UserRole } from "../types/auth";

export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  meta?: unknown;
};

export type AuthResponse = {
  token: string;
  tokenType: string;
  refreshToken: string;
  userId: string;
  email: string;
  fullName: string;
  role: UserRole;
  isVerified: boolean;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  address?: string;
};

export type VerifyEmailRequest = {
  email: string;
  otpCode: string;
};

export async function login(request: LoginRequest) {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>(
    "/api/v1/auth/login",
    request,
  );

  return data;
}

export async function registerCustomer(request: RegisterRequest) {
  const { data } = await apiClient.post<ApiResponse<null>>(
    "/api/v1/auth/register",
    request,
  );

  return data;
}

export async function registerOrganizer(request: RegisterRequest) {
  const { data } = await apiClient.post<ApiResponse<null>>(
    "/api/v1/auth/register/organizer",
    request,
  );

  return data;
}

export async function verifyEmail(request: VerifyEmailRequest) {
  const { data } = await apiClient.post<ApiResponse<null>>(
    "/api/v1/auth/verify-email",
    request,
  );

  return data;
}

export function toAuthSession(payload: AuthResponse): AuthSession {
  return {
    accessToken: payload.token,
    refreshToken: payload.refreshToken,
    tokenType: payload.tokenType,
    user: {
      id: payload.userId,
      email: payload.email,
      fullName: payload.fullName,
      role: payload.role,
      isVerified: payload.isVerified,
    },
  };
}

export function setAuthHeader(token: string | null) {
  if (token) {
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`;
    return;
  }

  delete apiClient.defaults.headers.common.Authorization;
}

export async function resendOtp(email: string) {
  const { data } = await apiClient.post<ApiResponse<null>>(
    "/api/v1/auth/resend-otp",
    { email },
  );
  return data;
}

export async function forgotPassword(email: string) {
  const { data } = await apiClient.post<ApiResponse<null>>(
    "/api/v1/auth/forgot-password",
    { email },
  );
  return data;
}

export async function resetPassword(request: {
  email: string;
  otpCode: string;
  newPassword: string;
}) {
  const { data } = await apiClient.post<ApiResponse<null>>(
    "/api/v1/auth/reset-password",
    request,
  );
  return data;
}

export async function logout(token: string) {
  const { data } = await apiClient.post<ApiResponse<null>>(
    "/api/v1/auth/logout",
    null,
    { headers: { Authorization: `Bearer ${token}` } },
  );
  return data;
}

// Maps known technical/English backend messages to user-friendly Vietnamese text.
// Checked as case-insensitive substrings so partial matches still translate.
const BACKEND_MESSAGE_MAP: Array<[string, string]> = [
  ["already in your cart", "Sản phẩm đã có trong giỏ hàng. Bạn có thể thay đổi số lượng ngay trong giỏ."],
  ["insufficient stock", "Số lượng tồn kho không đủ. Vui lòng chọn số lượng ít hơn."],
  ["out of stock", "Sản phẩm đã hết hàng."],
  ["not found", "Không tìm thấy thông tin yêu cầu. Vui lòng thử lại."],
  ["unauthorized", "Bạn cần đăng nhập để thực hiện thao tác này."],
  ["forbidden", "Bạn không có quyền thực hiện thao tác này."],
  ["already exists", "Dữ liệu này đã tồn tại trong hệ thống."],
  ["invalid otp", "Mã OTP không đúng hoặc đã hết hạn. Vui lòng thử lại."],
  ["otp expired", "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới."],
  ["email already", "Email này đã được đăng ký. Vui lòng đăng nhập hoặc dùng email khác."],
  ["password", "Mật khẩu không đúng. Vui lòng kiểm tra lại."],
  ["network error", "Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng và thử lại."],
];

function translateBackendMessage(raw: string): string {
  const lower = raw.toLowerCase();
  for (const [pattern, friendly] of BACKEND_MESSAGE_MAP) {
    if (lower.includes(pattern)) return friendly;
  }
  // Return the original if no translation matches — it may already be in Vietnamese.
  return raw;
}

export function getApiErrorMessage(
  error: unknown,
  fallbackMessage = "Đã xảy ra lỗi. Vui lòng thử lại.",
) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === "string" && message.trim().length > 0) {
      return translateBackendMessage(message.trim());
    }
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return translateBackendMessage(error.message.trim());
  }

  return fallbackMessage;
}
