import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { AuthSession } from "../types/auth";
import { withRefreshLock } from "../lib/refreshLock";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 20000,
});
export const authTransport = axios.create({
  baseURL: import.meta.env.PROD ? "" : import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
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
  revision?: () => number;
};
let bridge: Bridge | undefined;
let refreshing: Promise<AuthSession> | null = null;
export function registerAuthBridge(value: Bridge) {
  bridge = value;
}

export async function authPost<T>(path: string, payload: unknown = null) {
  const csrf = await authTransport.get("/api/v1/auth/csrf");
  const token = csrf.data?.data?.csrfToken;
  if (typeof token !== "string" || !token) throw new Error("Không thể xác minh yêu cầu.");
  return authTransport.post<T>(path, payload, { headers: { "X-CSRF-TOKEN": token } });
}
export function restoreSession(): Promise<AuthSession> { return refreshSession(true); }
export function refreshSession(bootstrap = false): Promise<AuthSession> {
  if (refreshing) return refreshing;
  const observed = bridge?.read();
  const revision = bridge?.revision?.();
  if (!observed && !bootstrap) return Promise.reject(new Error("Bạn cần đăng nhập lại."));
  const operation = withRefreshLock(async () => {
    await bridge?.sync();
    const current = bridge?.read();
    if (current?.user.id !== observed?.user.id || bridge?.revision?.() !== revision)
      throw new Error("Phiên đăng nhập đã thay đổi.");
    if (current && current.accessToken !== observed?.accessToken) return current;
    try {
      const { data } = await authPost<{ data: { token: string; userId: string; tokenType: string; email: string; fullName: string; role: AuthSession["user"]["role"]; isVerified: boolean } }>("/api/v1/auth/refresh");
      const p = data.data;
      if (!p?.token || !p.userId)
        throw new Error("Phản hồi đăng nhập không hợp lệ.");
      const session: AuthSession = {
        accessToken: p.token,
        tokenType: p.tokenType,
        user: {
          id: p.userId,
          email: p.email,
          fullName: p.fullName,
          role: p.role,
          isVerified: p.isVerified,
        },
      };
      if (bridge?.read()?.accessToken !== current?.accessToken || bridge?.read()?.user.id !== current?.user.id || bridge?.revision?.() !== revision)
        throw new Error("Phiên đăng nhập đã thay đổi.");
      if (current && session.user.id !== current.user.id) {
        bridge?.clear();
        throw new Error("Tài khoản đã thay đổi. Vui lòng đăng nhập lại.");
      }
      bridge?.write(session);
      return session;
    } catch (error) {
      if (
        current && axios.isAxiosError(error) &&
        [401, 403].includes(error.response?.status ?? 0) &&
        bridge?.read()?.accessToken === current?.accessToken
      )
        bridge?.clear();
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
