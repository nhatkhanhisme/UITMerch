import { useEffect, useRef, useState, useId } from "react";
import {
  Check,
  CheckCheck,
  ChevronLeft,
  ChevronRight,
  Inbox,
  ArrowUpRight,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { toast } from "../stores/toastStore";
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
  const [readFeedback, setReadFeedback] = useState("");
  const ref = useRef<HTMLDivElement>(null);
  const panelId = useId();
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
  function updateReadCache(id?: string) {
    qc.setQueriesData<{ items: NotificationResponse[] }>(
      { queryKey: key },
      (data) =>
        data && typeof data === "object" && "items" in data
          ? {
              ...data,
              items: data.items.map((n) =>
                !id || n.id === id ? { ...n, isRead: true } : n,
              ),
            }
          : data,
    );
  }
  const read = useMutation({
    mutationFn: async ({
      notification,
      openTarget,
    }: {
      notification: NotificationResponse;
      openTarget: boolean;
      userId: string;
      role: string;
    }) => {
      if (!notification.isRead)
        await apiClient.patch(`${prefix}/${notification.id}/read`);
      return openTarget
        ? notificationTarget(notification, organizer)
        : undefined;
    },
    onSuccess: (target, request) => {
      const current = useAuthStore.getState().user;
      if (current?.id !== request.userId || current.role !== request.role)
        return;
      updateReadCache(request.notification.id);
      if (!request.notification.isRead) {
        qc.setQueryData<number>([...key, "count"], (value) =>
          typeof value === "number" ? Math.max(0, value - 1) : value,
        );
        setReadFeedback("Đã đánh dấu thông báo là đã đọc.");
      }
      refresh();
      if (target) {
        setOpen(false);
        navigate(target);
      }
    },
  });
  const all = useMutation({
    mutationFn: (_request: { userId: string; role: string }) =>
      apiClient.patch(`${prefix}/read-all`),
    onSuccess: (_result, request) => {
      const current = useAuthStore.getState().user;
      if (current?.id !== request.userId || current.role !== request.role)
        return;
      updateReadCache();
      qc.setQueryData([...key, "count"], 0);
      setReadFeedback("Đã đánh dấu tất cả là đã đọc.");
      toast.success("Đã đánh dấu tất cả thông báo là đã đọc.");
      refresh();
    },
  });
  if (!enabled) return null;
  return (
    <div ref={ref} className="relative shrink-0">
      <button
        type="button"
        aria-label="Thông báo"
        aria-expanded={open}
        aria-controls={panelId}
        className="relative flex size-11 items-center justify-center rounded-full text-black-blue hover:bg-white/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-700"
        onClick={() => {
          setReadFeedback("");
          setOpen((v) => !v);
        }}
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
          <span
            aria-label="Chưa đọc"
            title={`${count.data} thông báo chưa đọc`}
            className="absolute right-0 top-0.5 min-w-4 rounded-full bg-cyan-800 px-1 text-center text-[10px] font-semibold leading-4 text-white"
          >
            {(count.data ?? 0) > 99 ? "99+" : count.data}
          </span>
        )}
      </button>
      {open && (
        <section
          style={{
            right: Math.min(
              0,
              (ref.current?.getBoundingClientRect().right ??
                window.innerWidth) -
                Math.min(380, window.innerWidth - 24) -
                12,
            ),
          }}
          className="notification-panel"
          id={panelId}
          aria-label="Danh sách thông báo"
        >
          <header className="notification-panel-header">
            <div>
              <h2>Thông báo</h2>
              <p>
                {count.isPending
                  ? "Đang kiểm tra thông báo…"
                  : count.isError
                    ? "Chưa tải được trạng thái đã đọc"
                    : (count.data ?? 0) > 0
                      ? `${count.data} thông báo chưa đọc`
                      : "Không còn thông báo chưa đọc"}
              </p>
            </div>
            <Inbox aria-hidden="true" size={23} />
          </header>
          <div className="notification-toolbar">
            <span>Mở thông báo để xem chi tiết.</span>
            <button
              type="button"
              className="notification-read-all"
              disabled={
                all.isPending ||
                read.isPending ||
                count.isPending ||
                count.isError ||
                (count.data ?? 0) === 0
              }
              onClick={() => all.mutate({ userId: user!.id, role: user!.role })}
              title="Đánh dấu tất cả đã đọc"
              aria-label="Đánh dấu tất cả đã đọc"
            >
              <CheckCheck aria-hidden="true" size={17} />
              <span>
                {all.isPending
                  ? "Đang đánh dấu…"
                  : !count.isPending &&
                      !count.isError &&
                      (count.data ?? 0) === 0
                    ? "Đã đọc tất cả"
                    : "Đọc tất cả"}
              </span>
            </button>
          </div>
          {readFeedback && (
            <p className="notification-confirmation" role="status">
              <CheckCheck aria-hidden="true" size={16} />
              {readFeedback}
            </p>
          )}
          {(q.isError || count.isError) && (
            <FeatureError error={q.error ?? count.error} retry={refresh} />
          )}
          {read.isError && <FeatureError error={read.error} />}
          {all.isError && <FeatureError error={all.error} />}
          {q.isPending ? (
            <p className="notification-empty" role="status">
              Đang tải thông báo…
            </p>
          ) : (
            <div className="notification-list">
              {q.data?.items.length === 0 && (
                <div className="notification-empty">
                  <Inbox aria-hidden="true" size={28} />
                  <p>Chưa có thông báo.</p>
                  <span>
                    Các cập nhật về đơn hàng và tổ chức sẽ xuất hiện tại đây.
                  </span>
                </div>
              )}
              {q.data?.items.map((n) => (
                <article
                  className={`notification-item ${n.isRead ? "is-read" : "is-unread"}`}
                  key={n.id}
                >
                  <button
                    className="notification-open"
                    type="button"
                    disabled={read.isPending || all.isPending}
                    onClick={() =>
                      read.mutate({
                        notification: n,
                        openTarget: true,
                        userId: user!.id,
                        role: user!.role,
                      })
                    }
                  >
                    <span className="notification-dot" aria-hidden="true" />
                    <span className="min-w-0 notification-content">
                      <span className="notification-title">{n.title}</span>
                      <span className="notification-message">{n.message}</span>
                      <span className="notification-meta">
                        <span
                          className={`notification-read-state ${n.isRead ? "read" : "unread"}`}
                        >
                          {n.isRead ? (
                            <Check aria-hidden="true" size={13} />
                          ) : (
                            <span aria-hidden="true" />
                          )}
                          {n.isRead ? "Đã đọc" : "Chưa đọc"}
                        </span>
                        {n.createdAt && (
                          <time
                            className="notification-time"
                            dateTime={n.createdAt}
                          >
                            {dateTime(n.createdAt)}
                          </time>
                        )}
                      </span>
                      {notificationTarget(n, organizer) && (
                        <span className="notification-open-hint">
                          Xem chi tiết{" "}
                          <ArrowUpRight aria-hidden="true" size={13} />
                        </span>
                      )}
                    </span>
                  </button>
                  {!n.isRead && (
                    <button
                      className="notification-read-one"
                      type="button"
                      aria-label={`Đánh dấu đã đọc: ${n.title}`}
                      title="Đánh dấu đã đọc"
                      disabled={read.isPending || all.isPending}
                      onClick={() =>
                        read.mutate({
                          notification: n,
                          openTarget: false,
                          userId: user!.id,
                          role: user!.role,
                        })
                      }
                    >
                      <Check aria-hidden="true" size={18} />
                    </button>
                  )}
                </article>
              ))}
            </div>
          )}
          <p className="notification-reading-note">
            Mở hoặc đánh dấu một thông báo sẽ chuyển nó sang đã đọc.
          </p>
          {(page > 0 || q.data?.meta?.hasNext) && (
            <nav
              className="notification-pagination"
              aria-label="Phân trang thông báo"
            >
              <button
                aria-label="Trang thông báo trước"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                <ChevronLeft aria-hidden="true" size={18} /> Trước
              </button>
              <span>Trang {page + 1}</span>
              <button
                disabled={!q.data?.meta?.hasNext}
                aria-label="Trang thông báo sau"
                onClick={() => setPage((p) => p + 1)}
              >
                Sau <ChevronRight aria-hidden="true" size={18} />
              </button>
            </nav>
          )}
        </section>
      )}
    </div>
  );
}
