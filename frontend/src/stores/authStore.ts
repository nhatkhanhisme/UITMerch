import { create } from "zustand";
import type { AuthSession, AuthUser } from "../types/auth";
import { apiClient, registerAuthBridge } from "../api/client";
import { resetServerState } from "../lib/queryClient";
import { cacheClear } from "../lib/sessionCache";
import { useCartStore } from "./cartStore";

type AuthState = {
  user: AuthUser | null;
  accessToken: string | null;
  tokenType: string | null;
  setSession: (session: AuthSession | null) => void;
  updateUser: (updates: Partial<AuthUser>) => void;
  clearSession: () => void;
};
function clearPrivateData(preserveBuyer?: string) {
  resetServerState();
  cacheClear();
  useCartStore.getState().clearCart();
  try {
    for (let i = sessionStorage.length - 1; i >= 0; i--) {
      const key = sessionStorage.key(i);
      if (key?.startsWith("uitmerch-reservation:") || key?.startsWith("uitmerch-checkout:")) {
        const ownIntent = preserveBuyer && (key.startsWith(`uitmerch-reservation:${preserveBuyer}:`) || key.startsWith(`uitmerch-checkout:${preserveBuyer}:`));
        if (!ownIntent) sessionStorage.removeItem(key);
      }
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
// Discard credentials persisted by older releases; never hydrate them.
try { localStorage.removeItem("uitmerch-auth"); } catch { /* Disabled storage is supported. */ }
let authRevision = 0;
export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null, accessToken: null, tokenType: null,
  setSession: session => {
    authRevision++;
    if (get().user?.id !== session?.user.id) clearPrivateData(!get().user ? session?.user.id : undefined);
    set({ user: session?.user ?? null, accessToken: session?.accessToken ?? null, tokenType: session?.tokenType ?? null });
    setHeader(session?.accessToken ?? null);
  },
  updateUser: updates => set(state => state.user ? { user: { ...state.user, ...updates } } : {}),
  clearSession: () => {
    authRevision++;
    clearPrivateData();
    set({ user: null, accessToken: null, tokenType: null });
    setHeader(null);
  },
}));
export function readSession(): AuthSession | null {
  const s = useAuthStore.getState();
  return s.user && s.accessToken ? { user: s.user, accessToken: s.accessToken, tokenType: s.tokenType ?? "Bearer" } : null;
}
registerAuthBridge({ read: readSession, write: s => useAuthStore.getState().setSession(s),
  clear: () => useAuthStore.getState().clearSession(), sync: async () => {}, revision: () => authRevision });
// Broadcast only invalidation, never credentials or private user data.
if (typeof window !== "undefined") window.addEventListener("storage", event => {
  if (event.key === "uitmerch-auth-invalidated") useAuthStore.getState().clearSession();
});
