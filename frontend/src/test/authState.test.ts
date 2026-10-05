import { beforeEach, expect, it } from "vitest";
import { useAuthStore } from "../stores/authStore";
import { useCartStore } from "../stores/cartStore";
import { queryClient } from "../lib/queryClient";
import { apiClient } from "../api/client";
import type { AuthSession } from "../types/auth";
const initial: AuthSession = {
  user: {
    id: "a",
    email: "a@uit.edu.vn",
    fullName: "A",
    role: "CUSTOMER",
    isVerified: true,
  },
  accessToken: "old",
  tokenType: "Bearer",
};
beforeEach(() => {
  useAuthStore.getState().setSession(initial);
  queryClient.setQueryData(["private", "a"], { secret: "private" });
  useCartStore
    .getState()
    .setItems([{ productId: "m", quantity: 1, price: 100 }]);
  sessionStorage.setItem("uitmerch:private", JSON.stringify({ user: "a" }));
  sessionStorage.setItem("uitmerch-reservation:a:c", "intent");
});
it("clears all private data and auth headers on logout", () => {
  useAuthStore.getState().clearSession();
  expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  expect(useCartStore.getState().items).toHaveLength(0);
  expect(sessionStorage.getItem("uitmerch:private")).toBeNull();
  expect(sessionStorage.getItem("uitmerch-reservation:a:c")).toBeNull();
  expect(apiClient.defaults.headers.common.Authorization).toBeUndefined();
});
it("clears private data when a different account signs in", () => {
  useAuthStore
    .getState()
    .setSession({ ...initial, user: { ...initial.user, id: "b" } });
  expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  expect(useCartStore.getState().items).toHaveLength(0);
  expect(sessionStorage.length).toBe(0);
});
it("preserves same-user data during token rotation", () => {
  useAuthStore
    .getState()
    .setSession({
      ...initial,
      accessToken: "new",
    });
  expect(queryClient.getQueryData(["private", "a"])).toEqual({
    secret: "private",
  });
  expect(useCartStore.getState().items).toHaveLength(1);
  expect(apiClient.defaults.headers.common.Authorization).toBe("Bearer new");
});
it("bootstraps the same buyer without discarding request IDs, then clears them on logout", () => {
  useAuthStore.getState().clearSession();
  sessionStorage.setItem("uitmerch-reservation:a:c", "own-request");
  sessionStorage.setItem("uitmerch-checkout:a:hash", "own-checkout");
  sessionStorage.setItem("uitmerch-reservation:other:c", "foreign-request");
  useAuthStore.getState().setSession(initial);
  expect(sessionStorage.getItem("uitmerch-reservation:a:c")).toBe("own-request");
  expect(sessionStorage.getItem("uitmerch-checkout:a:hash")).toBe("own-checkout");
  expect(sessionStorage.getItem("uitmerch-reservation:other:c")).toBeNull();
  expect(localStorage.getItem("uitmerch-auth")).toBeNull();
  useAuthStore.getState().clearSession();
  expect(sessionStorage.getItem("uitmerch-checkout:a:hash")).toBeNull();
});
