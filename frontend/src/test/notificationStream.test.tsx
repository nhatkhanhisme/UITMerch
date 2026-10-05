import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useNotificationStream, tokenExpiresAt } from "../hooks/useNotificationStream";
const state = vi.hoisted(() => ({ token: "" }));
vi.mock("../stores/authStore", () => ({ useAuthStore: Object.assign((select: (s: { accessToken: string }) => unknown) => select({ accessToken: state.token }), { getState: () => ({ accessToken: state.token }) }) }));
const refresh = vi.hoisted(() => vi.fn());
vi.mock("../api/client", () => ({ refreshSession: refresh }));
const encode = (exp: number) => `header.${btoa(JSON.stringify({ exp }))}.signature`;
const requests: Array<{ url: string; options: RequestInit; source: ReadableStreamDefaultController<Uint8Array> }> = [];
beforeEach(() => {
  vi.useFakeTimers(); requests.length = 0;
  vi.stubGlobal("fetch", vi.fn((url: string, options: RequestInit) => {
    let source!: ReadableStreamDefaultController<Uint8Array>;
    const body = new ReadableStream<Uint8Array>({ start(controller) { source = controller; } });
    options.signal?.addEventListener("abort", () => { try { source.error(new Error("Aborted")); } catch {} });
    requests.push({ url, options, source });
    return Promise.resolve({ ok: true, status: 200, body, headers: new Headers({ "content-type": "text/event-stream" }) });
  }));
  state.token = encode(Math.floor(Date.now() / 1000) + 3600);
  refresh.mockReset().mockResolvedValue({});
});
afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals(); });
it("parses expiry and treats malformed tokens as expired", () => {
  expect(tokenExpiresAt(encode(123))).toBe(123000); expect(tokenExpiresAt("invalid")).toBe(0);
});
it("sends credentials only in the header and aborts on rotation or logout", async () => {
  const { rerender, unmount } = renderHook(() => useNotificationStream({ path: "/stream", enabled: true, onMessage: () => {} }));
  await act(async () => {});
  expect(requests[0].url).not.toContain(state.token); expect(requests[0].url).not.toContain("?");
  expect(requests[0].options.headers).toMatchObject({ Authorization: `Bearer ${state.token}` });
  state.token = encode(Math.floor(Date.now() / 1000) + 7200); rerender();
  await act(async () => {});
  expect(requests[0].options.signal?.aborted).toBe(true); expect(requests).toHaveLength(2);
  unmount(); expect(requests[1].options.signal?.aborted).toBe(true);
});
it("handles split CRLF frames, UTF-8, multiline data and malformed JSON", async () => {
  const receive = vi.fn(), reconcile = vi.fn();
  const { unmount } = renderHook(() => useNotificationStream({ path: "/stream", enabled: true, onMessage: receive, onOpen: reconcile }));
  await act(async () => {});
  const bytes = new TextEncoder().encode('event: notification\r\ndata: {"name":"á",\r\ndata: "id":"one"}\r\n\r\nevent: notification\ndata: invalid\n\n: heartbeat\n\n');
  await act(async () => { for (let i = 0; i < bytes.length; i++) requests[0].source.enqueue(bytes.slice(i, i+1)); });
  expect(reconcile).toHaveBeenCalledOnce(); expect(receive).toHaveBeenCalledExactlyOnceWith({ name: "á", id: "one" }); unmount();
});
it("refreshes an expired token before opening a stream", async () => {
  state.token = encode(Math.floor(Date.now() / 1000) - 1);
  const { unmount } = renderHook(() => useNotificationStream({ path: "/stream", enabled: true, onMessage: () => {} }));
  await act(async () => {}); expect(refresh).toHaveBeenCalledOnce(); expect(requests).toHaveLength(0); unmount();
});
it("refreshes before expiry and cancels retry timers after logout", async () => {
  state.token = encode(Math.floor(Date.now() / 1000) + 40);
  const { unmount } = renderHook(() => useNotificationStream({ path: "/stream", enabled: true, onMessage: () => {} }));
  await act(async () => {});
  await act(async () => { await vi.advanceTimersByTimeAsync(11000); });
  expect(refresh).toHaveBeenCalledOnce(); unmount();
  await act(async () => { await vi.advanceTimersByTimeAsync(60000); }); expect(requests).toHaveLength(1);
});
it("reconnects after EOF and rejects oversized frames", async () => {
  const { unmount } = renderHook(() => useNotificationStream({ path: "/stream", enabled: true, onMessage: () => {} }));
  await act(async () => { requests[0].source.close(); });
  await act(async () => { await vi.advanceTimersByTimeAsync(2000); }); expect(requests).toHaveLength(2);
  await act(async () => { requests[1].source.enqueue(new Uint8Array(262145)); });
  expect(requests[1].options.signal?.aborted).toBe(true); unmount();
});
