import { beforeEach, afterEach, describe, expect, it, vi } from "vitest";
import {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import {
  apiClient,
  authTransport,
  refreshSession,
  registerAuthBridge,
} from "../api/client";
import type { AuthSession } from "../types/auth";
const initial: AuthSession = {
  user: {
    id: "customer-a",
    email: "a@uit.edu.vn",
    fullName: "A",
    role: "CUSTOMER",
    isVerified: true,
  },
  accessToken: "old-access",
  refreshToken: "old-refresh",
  tokenType: "Bearer",
};
const reply = (
  config: InternalAxiosRequestConfig,
  data: unknown,
  status = 200,
): AxiosResponse => ({
  config,
  data,
  status,
  statusText: String(status),
  headers: {},
});
function reject(config: InternalAxiosRequestConfig, status: number): never {
  throw new AxiosError(
    "Rejected",
    "ERR_BAD_RESPONSE",
    config,
    undefined,
    reply(config, { message: "Rejected" }, status),
  );
}
let session: AuthSession | null;
let sync: ReturnType<typeof vi.fn>;
let write: ReturnType<typeof vi.fn>;
let clear: ReturnType<typeof vi.fn>;
const rotate = (config: InternalAxiosRequestConfig) =>
  reply(config, {
    data: {
      token: "new-access",
      refreshToken: "new-refresh",
      userId: "customer-a",
      email: initial.user.email,
      fullName: "A",
      role: "CUSTOMER",
      isVerified: true,
      tokenType: "Bearer",
    },
  });
beforeEach(() => {
  session = structuredClone(initial);
  sync = vi.fn(async () => {});
  write = vi.fn((s) => {
    session = s;
  });
  clear = vi.fn(() => {
    session = null;
  });
  registerAuthBridge({ read: () => session, write, clear, sync });
  Object.defineProperty(navigator, "locks", {
    configurable: true,
    value: {
      request: async (_: string, work: () => Promise<unknown>) => work(),
    },
  });
});
afterEach(() => {
  delete apiClient.defaults.adapter;
  delete authTransport.defaults.adapter;
});
describe("session refresh", () => {
  it("coalesces concurrent 401s into one rotating refresh and retries each request", async () => {
    const refresh = vi.fn(async (c: InternalAxiosRequestConfig) => {
      await new Promise((r) => setTimeout(r, 10));
      return rotate(c);
    });
    authTransport.defaults.adapter = refresh;
    apiClient.defaults.adapter = async (c) => {
      if (c.headers.get("Authorization") === "Bearer old-access")
        return reject(c, 401);
      return reply(c, { ok: true });
    };
    const result = await Promise.all([
      apiClient.get("/private/a"),
      apiClient.get("/private/b"),
      apiClient.get("/private/c"),
    ]);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(result.every((r) => r.data.ok)).toBe(true);
    expect(session?.refreshToken).toBe("new-refresh");
  });
  it("rehydrates and reuses a token rotated by another tab", async () => {
    sync.mockImplementation(async () => {
      session = {
        ...initial,
        accessToken: "tab-access",
        refreshToken: "tab-refresh",
      };
    });
    const refresh = vi.fn();
    authTransport.defaults.adapter = refresh;
    expect((await refreshSession()).accessToken).toBe("tab-access");
    expect(refresh).not.toHaveBeenCalled();
  });
  it("never resurrects a session if logout occurs during refresh", async () => {
    let release: () => void = () => {};
    const wait = new Promise<void>((r) => {
      release = r;
    });
    authTransport.defaults.adapter = async (c) => {
      await wait;
      return rotate(c);
    };
    const pending = refreshSession();
    await vi.waitFor(() => expect(sync).toHaveBeenCalled());
    session = null;
    release();
    await expect(pending).rejects.toThrow("Phiên đăng nhập đã thay đổi");
    expect(write).not.toHaveBeenCalled();
  });
  it("clears a definitively revoked refresh token", async () => {
    authTransport.defaults.adapter = async (c) => reject(c, 401);
    await expect(refreshSession()).rejects.toBeInstanceOf(AxiosError);
    expect(clear).toHaveBeenCalledOnce();
    expect(session).toBeNull();
  });
  it("keeps the session after a transient refresh network failure", async () => {
    authTransport.defaults.adapter = async (c) => {
      throw new AxiosError("Network", "ERR_NETWORK", c);
    };
    await expect(refreshSession()).rejects.toBeInstanceOf(AxiosError);
    expect(clear).not.toHaveBeenCalled();
    expect(session?.refreshToken).toBe("old-refresh");
  });
  it("does not rotate on forbidden responses or auth failures", async () => {
    const refresh = vi.fn();
    authTransport.defaults.adapter = refresh;
    apiClient.defaults.adapter = async (c) =>
      reject(c, c.url?.startsWith("/api/v1/auth/") ? 401 : 403);
    await expect(apiClient.get("/private")).rejects.toBeInstanceOf(AxiosError);
    await expect(apiClient.post("/api/v1/auth/login")).rejects.toBeInstanceOf(
      AxiosError,
    );
    expect(refresh).not.toHaveBeenCalled();
  });
  it("retries a protected request at most once", async () => {
    const refresh = vi.fn(async (c) => rotate(c));
    authTransport.defaults.adapter = refresh;
    const protectedCall = vi.fn(async (c) => reject(c, 401));
    apiClient.defaults.adapter = protectedCall;
    await expect(apiClient.get("/private")).rejects.toBeInstanceOf(AxiosError);
    expect(refresh).toHaveBeenCalledOnce();
    expect(protectedCall).toHaveBeenCalledTimes(2);
  });
  it("does not overwrite a different account that signs in while refresh is pending", async () => {
    let release: () => void = () => {};
    const wait = new Promise<void>((r) => {
      release = r;
    });
    authTransport.defaults.adapter = async (c) => {
      await wait;
      return rotate(c);
    };
    const pending = refreshSession();
    await vi.waitFor(() => expect(sync).toHaveBeenCalled());
    session = {
      ...initial,
      user: { ...initial.user, id: "other-user" },
      refreshToken: "other-refresh",
    };
    release();
    await expect(pending).rejects.toThrow();
    expect(session.user.id).toBe("other-user");
    expect(write).not.toHaveBeenCalled();
  });
});

it("never retries a request from the previous account with a new account token", async () => {
  let release: () => void = () => {};
  const wait = new Promise<void>((r) => {
    release = r;
  });
  const refresh = vi.fn();
  authTransport.defaults.adapter = refresh;
  const calls = vi.fn(async (config: InternalAxiosRequestConfig) => {
    await wait;
    return reject(config, 401);
  });
  apiClient.defaults.adapter = calls;
  const pending = apiClient.post("/private/order", { quantity: 1 });
  await vi.waitFor(() => expect(calls).toHaveBeenCalledOnce());
  session = {
    ...initial,
    user: { ...initial.user, id: "other" },
    accessToken: "other-access",
    refreshToken: "other-refresh",
  };
  release();
  await expect(pending).rejects.toBeInstanceOf(AxiosError);
  expect(calls).toHaveBeenCalledOnce();
  expect(refresh).not.toHaveBeenCalled();
});

it("does not send a stale default Authorization header after logout", async () => {
  session = null;
  apiClient.defaults.headers.common.Authorization = "Bearer old-access";
  apiClient.defaults.adapter = async (config) =>
    reply(config, { authorization: config.headers.get("Authorization") });
  expect(
    (await apiClient.get("/api/v1/public/orders")).data.authorization,
  ).toBeUndefined();
  delete apiClient.defaults.headers.common.Authorization;
});
