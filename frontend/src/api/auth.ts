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

export function getApiErrorMessage(
  error: unknown,
  fallbackMessage = "Something went wrong. Please try again.",
) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === "string" && message.trim().length > 0) {
      return message;
    }
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }

  return fallbackMessage;
}
