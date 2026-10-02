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
    let stream: EventSource | undefined;
    let retry: ReturnType<typeof setTimeout> | undefined;
    let expiry: ReturnType<typeof setTimeout> | undefined;
    let attempt = 0;
    const base = (
      (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? ""
    ).replace(/\/$/, "");
    async function connect() {
      if (!active) return;
      clearTimeout(expiry);
      stream?.close();
      let current = useAuthStore.getState().accessToken;
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
      stream = new EventSource(
        `${base}${path}?token=${encodeURIComponent(current)}`,
      );
      stream.onopen = () => {
        attempt = 0;
        handlers.current.onOpen?.();
      };
      stream.addEventListener("notification", (event: MessageEvent) => {
        if (!active) return;
        try {
          handlers.current.onMessage(JSON.parse(event.data));
        } catch {
          /* Ignore malformed frames. */
        }
      });
      stream.onerror = () => {
        stream?.close();
        if (active)
          retry = setTimeout(connect, Math.min(2000 * 2 ** attempt++, 30000));
      };
      expiry = setTimeout(
        () => {
          stream?.close();
          void refreshSession().catch(() => {
            if (active) retry = setTimeout(connect, 2000);
          });
        },
        Math.max(0, tokenExpiresAt(current) - Date.now() - 30000),
      );
    }
    void connect();
    return () => {
      active = false;
      stream?.close();
      clearTimeout(retry);
      clearTimeout(expiry);
    };
  }, [enabled, token, path]);
}
