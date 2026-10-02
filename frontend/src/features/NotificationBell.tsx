import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { apiClient } from "../api/client";
import { useAuthStore } from "../stores/authStore";
import { useNotificationStream } from "../hooks/useNotificationStream";
import {
  normalizePage,
  normalizeNotification,
  type WireNotification,
  dateTime,
} from "../lib/featureUtils";
import { invalidateFeatures } from "../lib/queryClient";
import { FeatureError } from "./FeatureUI";
import type { ApiResponse, NotificationResponse } from "../types/shared";
import type { SpringPage } from "../types/features";
export function notificationTarget(
  n: NotificationResponse,
  organizer: boolean,
) {
  if (n.relatedOrderId)
    return organizer
      ? `/organizer?tab=orders${n.relatedOrgId ? `&orgId=${n.relatedOrgId}` : ""}&orderId=${n.relatedOrderId}`
      : `/orders/${n.relatedOrderId}`;
  if (n.relatedMerchId) return `/merch/${n.relatedMerchId}`;
  if (n.relatedEventId) return `/event/${n.relatedEventId}`;
  if (n.relatedOrgId) return `/organization/${n.relatedOrgId}`;
  return undefined;
}
export function NotificationBell() {
  const user = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const seen = useRef(new Set<string>());
  const organizer = user?.role === "ORGANIZER";
  const enabled = user?.role === "CUSTOMER" || organizer;
  const prefix = organizer
    ? "/api/v1/organizer/notifications"
    : "/api/v1/customer/notifications";
  const key = ["notifications", user?.id, user?.role];
  const q = useQuery({
    queryKey: [...key, page],
    queryFn: async () => {
      const result = normalizePage(
        (
          await apiClient.get<
            ApiResponse<WireNotification[] | SpringPage<WireNotification>>
          >(prefix, { params: { page, size: 20 } })
        ).data,
      );
      return { ...result, items: result.items.map(normalizeNotification) };
    },
    enabled: enabled && open,
  });
  const count = useQuery({
    queryKey: [...key, "count"],
    queryFn: async () =>
      (
        await apiClient.get<ApiResponse<{ unreadCount: number }>>(
          `${prefix}/unread-count`,
        )
      ).data.data.unreadCount,
    enabled,
  });
  function refresh() {
    void qc.invalidateQueries({ queryKey: key });
  }
  useEffect(() => {
    seen.current.clear();
    setOpen(false);
    setPage(0);
  }, [user?.id, user?.role]);
  useEffect(() => setOpen(false), [location]);
  useEffect(() => {
    if (!open) return;
    const close = (e: MouseEvent) => {
      if (!ref.current?.contains(e.target as Node)) setOpen(false);
    };
    const escape = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", escape);
    };
  }, [open]);
  useNotificationStream({
    path: `${prefix}/stream`,
    enabled,
    onOpen: refresh,
    onMessage: (data) => {
      if (!data || typeof data !== "object") return;
      const event = data as Partial<NotificationResponse>;
      if (event.id) {
        if (seen.current.has(event.id)) return;
        seen.current.add(event.id);
        if (seen.current.size > 500)
          seen.current.delete(seen.current.values().next().value!);
      }
      // Organizer order frames carry order IDs, not persisted notification IDs: reload the REST list.
      refresh();
      invalidateFeatures(
        event.type?.startsWith("MERCH_")
          ? ["catalog"]
          : event.type?.startsWith("ORDER_") ||
              event.type === "NEW_ORDER" ||
              event.type === "PICKUP_SCHEDULED"
            ? ["orders", "campaigns"]
            : [],
      );
    },
  });
  const read = useMutation({
    mutationFn: async (n: NotificationResponse) => {
      if (!n.isRead) await apiClient.patch(`${prefix}/${n.id}/read`);
      return notificationTarget(n, organizer);
    },
    onSuccess: (target) => {
      refresh();
      if (target) {
        setOpen(false);
        navigate(target);
      }
    },
  });
  const all = useMutation({
    mutationFn: () => apiClient.patch(`${prefix}/read-all`),
    onSuccess: refresh,
  });
  if (!enabled) return null;
  return (
    <div ref={ref} className="relative mr-2">
      <button
        type="button"
        aria-label="Thông báo"
        aria-expanded={open}
        className="rounded-full p-2"
        onClick={() => setOpen((v) => !v)}
      >
        <svg
          aria-hidden="true"
          width="20"
          height="20"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M9 21h6" />
        </svg>
        {(count.data ?? 0) > 0 && (
          <span aria-label="Chưa đọc"> {count.data}</span>
        )}
      </button>
      {open && (
        <section
          style={{
            right: Math.min(
              0,
              (ref.current?.getBoundingClientRect().right ??
                window.innerWidth) -
                Math.min(340, window.innerWidth - 24) -
                12,
            ),
          }}
          className="absolute right-0 top-full z-50 w-[min(340px,calc(100vw-24px))] rounded-2xl bg-white p-4 shadow-xl"
        >
          <h2>Thông báo</h2>
          <div className="feature-actions">
            <button disabled={all.isPending} onClick={() => all.mutate()}>
              Đánh dấu tất cả đã đọc
            </button>
          </div>
          {(q.isError || count.isError) && (
            <FeatureError error={q.error ?? count.error} retry={refresh} />
          )}
          {read.isError && <FeatureError error={read.error} />}
          {all.isError && <FeatureError error={all.error} />}
          {q.isPending ? (
            <p>Đang tải…</p>
          ) : (
            <div className="max-h-72 overflow-y-auto">
              {q.data?.items.length === 0 && <p>Chưa có thông báo.</p>}
              {q.data?.items.map((n) => (
                <button
                  type="button"
                  disabled={read.isPending}
                  className={`block w-full border-b p-3 text-left ${n.isRead ? "opacity-70" : "font-semibold"}`}
                  key={n.id}
                  onClick={() => read.mutate(n)}
                >
                  <p>{n.title}</p>
                  <p>{n.message}</p>
                  {n.createdAt && <small>{dateTime(n.createdAt)}</small>}
                </button>
              ))}
            </div>
          )}
          <div className="feature-actions">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Trước
            </button>
            <span>Trang {page + 1}</span>
            <button
              disabled={!q.data?.meta?.hasNext}
              onClick={() => setPage((p) => p + 1)}
            >
              Sau
            </button>
          </div>
        </section>
      )}
    </div>
  );
}
