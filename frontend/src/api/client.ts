import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { AuthSession } from "../types/auth";
import { withRefreshLock } from "../lib/refreshLock";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 20000,
});
export const authTransport = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
});
// Use only for catalog reads whose response does not depend on the account.
export const publicClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 20000,
  withCredentials: false,
});
publicClient.interceptors.request.use(config => {
  config.headers.delete("Authorization");
  return config;
}, error => { throw error; }, { synchronous: true });
type Bridge = {
  read: () => AuthSession | null;
  write: (s: AuthSession) => void;
  clear: () => void;
  sync: () => Promise<void>;
};
let bridge: Bridge | undefined;
let refreshing: Promise<AuthSession> | null = null;
export function registerAuthBridge(value: Bridge) {
  bridge = value;
}

export function refreshSession(): Promise<AuthSession> {
  if (refreshing) return refreshing;
  const observed = bridge?.read();
  if (!observed) return Promise.reject(new Error("Bạn cần đăng nhập lại."));
  const operation = withRefreshLock(async () => {
    await bridge?.sync();
    const current = bridge?.read();
    if (!current || current.user.id !== observed.user.id)
      throw new Error("Phiên đăng nhập đã thay đổi.");
    if (current.refreshToken !== observed.refreshToken) return current;
    try {
      const { data } = await authTransport.post("/api/v1/auth/refresh", {
        refreshToken: current.refreshToken,
      });
      const p = data.data;
      if (!p?.token || !p.refreshToken || !p.userId)
        throw new Error("Phản hồi đăng nhập không hợp lệ.");
      const session: AuthSession = {
        accessToken: p.token,
        refreshToken: p.refreshToken,
        tokenType: p.tokenType,
        user: {
          id: p.userId,
          email: p.email,
          fullName: p.fullName,
          role: p.role,
          isVerified: p.isVerified,
        },
      };
      if (bridge?.read()?.refreshToken !== current.refreshToken)
        throw new Error("Phiên đăng nhập đã thay đổi.");
      bridge.write(session);
      return session;
    } catch (error) {
      if (
        axios.isAxiosError(error) &&
        [401, 403].includes(error.response?.status ?? 0) &&
        bridge?.read()?.refreshToken === current.refreshToken
      )
        bridge.clear();
      throw error;
    }
  });
  refreshing = operation;
  void operation
    .finally(() => {
      if (refreshing === operation) refreshing = null;
    })
    .catch(() => {});
  return operation;
}

type RetryConfig = InternalAxiosRequestConfig & {
  _authRetried?: boolean;
  _sessionUserId?: string;
};
apiClient.interceptors.request.use((config) => {
  const session = bridge?.read();
  const scoped = config as RetryConfig;
  if (bridge && !session && !config.url?.startsWith("/api/v1/auth/"))
    config.headers.delete("Authorization");
  if (scoped._sessionUserId && scoped._sessionUserId !== session?.user.id)
    throw new Error("Tài khoản đã thay đổi. Vui lòng thao tác lại.");
  if (session && !config.url?.startsWith("/api/v1/auth/")) {
    scoped._sessionUserId ??= session.user.id;
    config.headers.set("Authorization", `Bearer ${session.accessToken}`);
  }
  return config;
}, error => { throw error; }, { synchronous: true });
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryConfig | undefined;
    if (
      error.response?.status !== 401 ||
      !config ||
      config._authRetried ||
      config.url?.startsWith("/api/v1/auth/")
    )
      throw error;
    const current = bridge?.read();
    if (!current || config._sessionUserId !== current.user.id) throw error;
    config._authRetried = true;
    const failedToken = config.headers.get("Authorization");
    const session =
      failedToken !== `Bearer ${current.accessToken}`
        ? current
        : await refreshSession();
    config.headers.set("Authorization", `Bearer ${session.accessToken}`);
    return apiClient.request(config);
  },
);
