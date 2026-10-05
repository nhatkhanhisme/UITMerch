import { apiClient } from "./client";
import { useAuthStore } from "../stores/authStore";
const pending = new Map<string, string>();
/** Only a random request id is persisted under a hashed account/payload key. */
export async function withCheckoutRequest<T>(kind: string, payload: unknown, work: (requestId: string) => Promise<T>): Promise<T> {
  const stablePayload = payload && typeof payload === "object"
    ? Object.fromEntries(Object.entries(payload).filter(([key]) => key !== "guestCheckoutToken")) : payload;
  const buyer = useAuthStore.getState().user?.id ?? "guest";
  const input = JSON.stringify([buyer, kind, stablePayload]);
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input));
  const key = `uitmerch-checkout:${buyer}:` + Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, "0")).join("");
  let id = pending.get(key);
  try { id ??= sessionStorage.getItem(key) ?? undefined; } catch { /* memory fallback */ }
  id ??= crypto.randomUUID(); pending.set(key, id);
  try { sessionStorage.setItem(key, id); } catch { /* memory fallback */ }
  const result = await work(id);
  pending.delete(key);
  try { sessionStorage.removeItem(key); } catch { /* memory fallback */ }
  return result;
}
export async function requestCheckoutCode(email: string): Promise<string> {
  const response = await apiClient.post("/api/v1/public/checkout/challenge", { email });
  return response.data.data.challengeId;
}
export async function verifyCheckoutCode(email: string, challengeId: string, code: string): Promise<string> {
  const response = await apiClient.post("/api/v1/public/checkout/verify", { email, challengeId, code });
  return response.data.data.guestCheckoutToken;
}
