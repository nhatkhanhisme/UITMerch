import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthSession, AuthUser } from "../types/auth";
import { setAuthHeader } from "../api/auth";

type AuthState = {
  user: AuthUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string | null;
  setSession: (session: AuthSession | null) => void;
  clearSession: () => void;
};

const storageKey = "uitmerch-auth";

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      tokenType: null,
      setSession: (session) => {
        set({
          user: session?.user ?? null,
          accessToken: session?.accessToken ?? null,
          refreshToken: session?.refreshToken ?? null,
          tokenType: session?.tokenType ?? null,
        });
        setAuthHeader(session?.accessToken ?? null);
      },
      clearSession: () => {
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          tokenType: null,
        });
        setAuthHeader(null);
      },
    }),
    {
      name: storageKey,
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        tokenType: state.tokenType,
      }),
      onRehydrateStorage: () => (state) => {
        setAuthHeader(state?.accessToken ?? null);
      },
    },
  ),
);
