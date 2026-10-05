import { useEffect, useRef } from "react";
import { useAuthStore } from "../stores/authStore";
import { refreshSession } from "../api/client";
type Options = {
  path: string;
  onMessage: (data: unknown) => void;
  enabled: boolean;
  onOpen?: () => void;
};
export function tokenExpiresAt(token: string): number {
  try {
    return (
      JSON.parse(
        atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")),
      ).exp * 1000
    );
  } catch {
    return 0;
  }
}
export function useNotificationStream({
  path,
  onMessage,
  enabled,
  onOpen,
}: Options) {
  const token = useAuthStore((s) => s.accessToken);
  const handlers = useRef({ onMessage, onOpen });
  handlers.current = { onMessage, onOpen };
  useEffect(() => {
    if (!enabled || !token) return;
    let active = true;
    let stream: AbortController | undefined;
    let retry: ReturnType<typeof setTimeout> | undefined;
    let expiry: ReturnType<typeof setTimeout> | undefined;
    let attempt = 0;
    const base = (
      (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? ""
    ).replace(/\/$/, "");
    async function connect() {
      if (!active) return;
      clearTimeout(expiry);
      stream?.abort();
      const current = useAuthStore.getState().accessToken;
      if (!current) return;
      try {
        if (tokenExpiresAt(current) < Date.now() + 30000) {
          await refreshSession();
          return;
        }
      } catch {
        if (active)
          retry = setTimeout(connect, Math.min(2000 * 2 ** attempt++, 30000));
        return;
      }
      if (!active) return;
      const connection = new AbortController();
      stream = connection;
      expiry = setTimeout(() => {
        connection.abort();
        void refreshSession().catch(() => {});
      }, Math.max(0, tokenExpiresAt(current) - Date.now() - 30000));
      try {
        const response = await fetch(`${base}${path}`, {
          headers: { Authorization: `Bearer ${current}`, Accept: "text/event-stream" },
          credentials: "omit",
          cache: "no-store",
          signal: connection.signal,
        });
        if (!active || connection.signal.aborted) return;
        if (response.status === 401) {
          await refreshSession();
          return;
        }
        if (!response.ok || !response.body || !response.headers.get("content-type")?.includes("text/event-stream"))
          throw new Error("Notification stream unavailable");
        attempt = 0;
        handlers.current.onOpen?.();
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        try {
          while (active && !connection.signal.aborted) {
            const { value, done } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            if (buffer.length > 262144) throw new Error("Notification frame too large");
            let boundary: RegExpExecArray | null;
            while ((boundary = /\r?\n\r?\n/.exec(buffer))) {
              const frame = buffer.slice(0, boundary.index);
              buffer = buffer.slice(boundary.index + boundary[0].length);
              let event = "message";
              const data: string[] = [];
              for (const line of frame.split(/\r?\n/)) {
                if (line.startsWith("event:")) event = line.slice(6).trim();
                if (line.startsWith("data:")) data.push(line.slice(5).replace(/^ /, ""));
              }
              if (event === "notification" && active && !connection.signal.aborted) {
                try { handlers.current.onMessage(JSON.parse(data.join("\n"))); }
                catch { /* Ignore malformed frames. */ }
              }
            }
          }
        } finally {
          await reader.cancel().catch(() => {});
          reader.releaseLock();
        }
      } catch { /* Retry network, authentication and framing failures below. */ }
      finally {
        clearTimeout(expiry);
        connection.abort();
        if (active)
          retry = setTimeout(connect, Math.min(2000 * 2 ** attempt++, 30000));
      }
    }
    void connect();
    return () => {
      active = false;
      stream?.abort();
      clearTimeout(retry);
      clearTimeout(expiry);
    };
  }, [enabled, token, path]);
}
