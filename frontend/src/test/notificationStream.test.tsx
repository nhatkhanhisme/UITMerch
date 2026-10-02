import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { renderHook, act } from "@testing-library/react";
import {
  useNotificationStream,
  tokenExpiresAt,
} from "../hooks/useNotificationStream";
const state = vi.hoisted(() => ({ token: "" }));
vi.mock("../stores/authStore", () => ({
  useAuthStore: Object.assign(
    (select: (s: { accessToken: string }) => unknown) =>
      select({ accessToken: state.token }),
    { getState: () => ({ accessToken: state.token }) },
  ),
}));
const refresh = vi.hoisted(() => vi.fn());
vi.mock("../api/client", () => ({ refreshSession: refresh }));
const encode = (exp: number) =>
  `header.${btoa(JSON.stringify({ exp }))}.signature`;
class Stream {
  static instances: Stream[] = [];
  url: string;
  onerror: (() => void) | null = null;
  onopen: (() => void) | null = null;
  close = vi.fn();
  listener: ((e: { data: string }) => void) | undefined;
  constructor(url: string) {
    this.url = url;
    Stream.instances.push(this);
  }
  addEventListener(_: string, listener: (e: { data: string }) => void) {
    this.listener = listener;
  }
}
beforeEach(() => {
  vi.useFakeTimers();
  vi.stubGlobal("EventSource", Stream);
  Stream.instances = [];
  state.token = encode(Math.floor(Date.now() / 1000) + 3600);
  refresh.mockReset();
  refresh.mockResolvedValue({});
});
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});
it("parses expiry and treats malformed tokens as expired", () => {
  expect(tokenExpiresAt(encode(123))).toBe(123000);
  expect(tokenExpiresAt("invalid")).toBe(0);
});
it("closes an old stream when the access token rotates", async () => {
  const { rerender, unmount } = renderHook(() =>
    useNotificationStream({
      path: "/stream",
      enabled: true,
      onMessage: () => {},
    }),
  );
  await act(async () => {});
  const old = Stream.instances[0];
  state.token = encode(Math.floor(Date.now() / 1000) + 7200);
  rerender();
  await act(async () => {});
  expect(old.close).toHaveBeenCalled();
  expect(Stream.instances).toHaveLength(2);
  expect(Stream.instances[1].url).toContain(encodeURIComponent(state.token));
  unmount();
  expect(Stream.instances[1].close).toHaveBeenCalled();
});
it("reconciles on connect, delivers frames, ignores malformed JSON", async () => {
  const receive = vi.fn();
  const reconcile = vi.fn();
  renderHook(() =>
    useNotificationStream({
      path: "/stream",
      enabled: true,
      onMessage: receive,
      onOpen: reconcile,
    }),
  );
  await act(async () => {});
  const stream = Stream.instances[0];
  stream.onopen?.();
  stream.listener?.({ data: '{"id":"notification"}' });
  stream.listener?.({ data: "invalid" });
  expect(reconcile).toHaveBeenCalledOnce();
  expect(receive).toHaveBeenCalledOnce();
});
it("refreshes an expired token before creating a stream", async () => {
  state.token = encode(Math.floor(Date.now() / 1000) - 1);
  renderHook(() =>
    useNotificationStream({
      path: "/stream",
      enabled: true,
      onMessage: () => {},
    }),
  );
  await act(async () => {});
  expect(refresh).toHaveBeenCalledOnce();
  expect(Stream.instances).toHaveLength(0);
});
it("refreshes before expiry and cancels reconnect timers on logout", async () => {
  state.token = encode(Math.floor(Date.now() / 1000) + 40);
  const { unmount } = renderHook(() =>
    useNotificationStream({
      path: "/stream",
      enabled: true,
      onMessage: () => {},
    }),
  );
  await act(async () => {});
  await act(async () => {
    await vi.advanceTimersByTimeAsync(11000);
  });
  expect(refresh).toHaveBeenCalledOnce();
  Stream.instances[0].onerror?.();
  unmount();
  await act(async () => {
    await vi.advanceTimersByTimeAsync(60000);
  });
  expect(Stream.instances).toHaveLength(1);
});
