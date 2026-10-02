import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthSession, AuthUser } from "../types/auth";
import { apiClient, registerAuthBridge } from "../api/client";
import { resetServerState } from "../lib/queryClient";
import { cacheClear } from "../lib/sessionCache";
import { useCartStore } from "./cartStore";

type AuthState = {
  user: AuthUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string | null;
  setSession: (session: AuthSession | null) => void;
  updateUser: (updates: Partial<AuthUser>) => void;
  clearSession: () => void;
};
function clearPrivateData() {
  resetServerState();
  cacheClear();
  useCartStore.getState().clearCart();
  try {
    for (let i = sessionStorage.length - 1; i >= 0; i--) {
      const key = sessionStorage.key(i);
      if (key?.startsWith("uitmerch-reservation:"))
        sessionStorage.removeItem(key);
    }
  } catch {
    /* A disabled storage area must not prevent logout. */
  }
}
function setHeader(token: string | null) {
  if (token)
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`;
  else delete apiClient.defaults.headers.common.Authorization;
}
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      tokenType: null,
      setSession: (session) => {
        if (get().user?.id !== session?.user.id) clearPrivateData();
        set({
          user: session?.user ?? null,
          accessToken: session?.accessToken ?? null,
          refreshToken: session?.refreshToken ?? null,
          tokenType: session?.tokenType ?? null,
        });
        setHeader(session?.accessToken ?? null);
      },
      updateUser: (updates) =>
        set((state) =>
          state.user ? { user: { ...state.user, ...updates } } : {},
        ),
      clearSession: () => {
        clearPrivateData();
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          tokenType: null,
        });
        setHeader(null);
      },
    }),
    {
      name: "uitmerch-auth",
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        tokenType: state.tokenType,
      }),
      onRehydrateStorage: () => (state) =>
        setHeader(state?.accessToken ?? null),
    },
  ),
);
export function readSession(): AuthSession | null {
  const s = useAuthStore.getState();
  return s.user && s.accessToken && s.refreshToken
    ? {
        user: s.user,
        accessToken: s.accessToken,
        refreshToken: s.refreshToken,
        tokenType: s.tokenType ?? "Bearer",
      }
    : null;
}
registerAuthBridge({
  read: readSession,
  write: (s) => useAuthStore.getState().setSession(s),
  clear: () => useAuthStore.getState().clearSession(),
  sync: async () => {
    await useAuthStore.persist.rehydrate();
  },
});
if (typeof window !== "undefined")
  window.addEventListener("storage", async (event) => {
    if (event.key !== "uitmerch-auth") return;
    const previous = readSession();
    await useAuthStore.persist.rehydrate();
    if (previous?.user.id !== readSession()?.user.id || !readSession())
      clearPrivateData();
  });
