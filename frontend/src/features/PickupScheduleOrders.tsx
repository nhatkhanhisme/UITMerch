import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { checkInOrder, getPickupScheduleOrders } from "../api/order";
import { getApiErrorMessage } from "../api/auth";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { ApiResponse, OrderResponse } from "../types/shared";

export function PickupScheduleOrders({ orgId, scheduleId }: { orgId: string; scheduleId: string }) {
  const [page, setPage] = useState(0);
  const userId = useAuthStore(state => state.user?.id);
  const client = useQueryClient();
  const prefix = ["pickup-order-pages", userId, orgId, scheduleId];
  const key = [...prefix, page];
  const query = useQuery({
    queryKey: key,
    queryFn: ({ signal }) => getPickupScheduleOrders(orgId, scheduleId, { page, size: 20 }, signal),
    enabled: Boolean(userId),
  });
  const checkIn = useMutation({
    mutationFn: (orderId: string) => checkInOrder(orgId, orderId),
    onSuccess: async response => {
      if (useAuthStore.getState().user?.id !== userId) return;
      client.setQueryData<ApiResponse<OrderResponse[]>>(key, previous => previous && ({
        ...previous,
        data: previous.data.map(order => order.id === response.data.id ? response.data : order),
      }));
      toast.success("Đã check-in đơn hàng.");
      await client.invalidateQueries({ queryKey: prefix });
    },
    onError: error => toast.error(getApiErrorMessage(error)),
  });
  const orders = query.data?.data ?? [];
  const meta = query.data?.meta;
  const busy = query.isFetching || checkIn.isPending;

  return (
    <section aria-label="Danh sách check-in" aria-busy={busy} className="border-t border-white/40 p-5">
      <p className="mb-3 text-xs font-semibold uppercase text-ink/50">Danh sách check-in</p>
      {query.isPending ? (
        <p role="status" className="text-sm text-ink/60">Đang tải danh sách đơn hàng…</p>
      ) : query.isError ? (
        <div role="alert" className="space-y-3 text-sm text-ink/70">
          <p>Không thể tải danh sách check-in.</p>
          <button type="button" className="min-h-10 rounded-full border px-4" onClick={() => void query.refetch()} disabled={busy}>Thử lại</button>
        </div>
      ) : orders.length === 0 ? (
        <p className="text-xs text-ink/50">Không có đơn hàng trong trang này.</p>
      ) : (
        <div className="space-y-2">
          {orders.map(order => (
            <div key={order.id} className={`flex flex-wrap items-center justify-between gap-3 rounded-xl border px-4 py-3 text-sm ${order.status === "COMPLETED" ? "border-green-200 bg-green-50" : "border-white/50 bg-white/40"}`}>
              <div className="flex items-center gap-3">
                <span className="font-mono text-sm font-bold text-black-blue">#{order.id.slice(0, 8).toUpperCase()}</span>
                <div>
                  <p className="text-xs font-semibold text-black-blue">{order.guestName ?? "Khách hàng"}</p>
                  {order.guestPhone && <p className="text-xs text-ink/50">{order.guestPhone}</p>}
                </div>
              </div>
              {order.status === "COMPLETED" ? (
                <span className="rounded-full border border-green-300 bg-green-100 px-3 py-1 text-xs font-bold text-green-800">✓ Đã nhận</span>
              ) : order.status === "READY" ? (
                <button type="button" className="min-h-10 rounded-full border border-aqua/60 bg-aqua/20 px-4 text-xs font-bold text-black-blue hover:bg-aqua/40 disabled:opacity-50"
                  aria-label={`Check-in đơn ${order.id.slice(0, 8).toUpperCase()}`} disabled={busy} onClick={() => checkIn.mutate(order.id)}>
                  {checkIn.isPending && checkIn.variables === order.id ? "Đang check-in…" : "Check-in"}
                </button>
              ) : (
                <span className="text-xs text-ink/60">Chưa sẵn sàng nhận</span>
              )}
            </div>
          ))}
        </div>
      )}
      {(meta && meta.totalPages > 1 || page > 0) && (
        <nav aria-label="Phân trang danh sách check-in" className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
          <button type="button" className="min-h-10 rounded-full border px-4 disabled:opacity-50" disabled={page === 0 || busy} onClick={() => setPage(value => value - 1)}>Trang trước</button>
          <span aria-live="polite">Trang {page + 1}{meta ? ` / ${meta.totalPages} · ${meta.totalElements} đơn` : ""}</span>
          <button type="button" className="min-h-10 rounded-full border px-4 disabled:opacity-50" disabled={!meta?.hasNext || busy} onClick={() => setPage(value => value + 1)}>Trang sau</button>
        </nav>
      )}
    </section>
  );
}
