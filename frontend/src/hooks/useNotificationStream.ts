import { useEffect, useRef } from "react";
import { useAuthStore } from "../stores/authStore";

type Options = {
  path: string;
  onMessage: (data: unknown) => void;
  enabled: boolean;
};

export function useNotificationStream({ path, onMessage, enabled }: Options) {
  const accessToken = useAuthStore((state) => state.accessToken);
  const onMessageRef = useRef(onMessage);
  const retryRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const esRef = useRef<EventSource | null>(null);

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    if (!enabled || !accessToken) return;

    const baseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";
    let attempt = 0;
    let active = true;

    const connect = () => {
      if (!active) return;

      const url = `${baseUrl}${path}?token=${encodeURIComponent(accessToken)}`;
      const es = new EventSource(url);
      esRef.current = es;

      es.addEventListener("notification", (e: MessageEvent) => {
        try {
          onMessageRef.current(JSON.parse(e.data as string));
          attempt = 0;
        } catch {
          // ignore parse errors
        }
      });

      es.onerror = () => {
        es.close();
        esRef.current = null;
        if (!active) return;
        // Exponential backoff: 2 s → 4 s → 8 s … max 30 s
        const delay = Math.min(2_000 * 2 ** attempt, 30_000);
        attempt++;
        retryRef.current = setTimeout(connect, delay);
      };
    };

    connect();

    return () => {
      active = false;
      if (retryRef.current !== null) clearTimeout(retryRef.current);
      esRef.current?.close();
      esRef.current = null;
    };
  }, [enabled, accessToken, path]);
}
