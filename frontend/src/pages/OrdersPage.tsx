import { lazy, Suspense, useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { getApiErrorMessage } from "../api/auth";
import { cancelCustomerOrder, getCustomerOrders } from "../api/order";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { CancelOrderRequest, OrderResponse } from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency",
});

function formatPrice(price: number) {
  return currencyFormatter.format(price);
}

function formatDate(dateStr?: string) {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

const STATUS_TABS = [
  { label: "Tất cả", value: "" },
  { label: "Chờ xác nhận", value: "PENDING" },
  { label: "Đã xác nhận", value: "CONFIRMED" },
  { label: "Sẵn sàng nhận", value: "READY" },
  { label: "Hoàn thành", value: "COMPLETED" },
  { label: "Đã huỷ", value: "CANCELLED" },
];

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  READY: "Sẵn sàng nhận",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã huỷ",
};

const STATUS_COLORS: Record<string, string> = {
  PENDING: "bg-gold/20 text-black-blue border-gold/40",
  CONFIRMED: "bg-aqua/20 text-black-blue border-aqua/40",
  READY: "bg-green-100 text-green-800 border-green-200",
  COMPLETED: "bg-green-200 text-green-900 border-green-300",
  CANCELLED: "bg-peach/20 text-black-blue border-peach/40",
};

function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={[
        "inline-flex items-center rounded-full border px-3 py-0.5 text-xs font-semibold",
        STATUS_COLORS[status] || "bg-white/40 text-ink border-white/40",
      ].join(" ")}
    >
      {STATUS_LABELS[status] || status}
    </span>
  );
}

// ─── Cancel Modal ─────────────────────────────────────────────────────────────

const CUSTOMER_CANCEL_REASONS = [
  "Tôi đặt nhầm sản phẩm / số lượng",
  "Tôi không thể đến nhận hàng đúng lịch",
  "Tôi muốn đổi sang sản phẩm khác",
  "Tôi không còn nhu cầu nữa",
  "Lý do khác",
] as const;

function CancelOrderModal({
  onConfirm,
  onClose,
  isLoading,
}: {
  onConfirm: (req: CancelOrderRequest) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const [selectedReason, setSelectedReason] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string | null>(null);

  const isOther = selectedReason === "Lý do khác";

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!selectedReason) { setError("Vui lòng chọn lý do huỷ đơn."); return; }
    if (isOther && note.trim().length < 10) { setError("Lý do khác phải có ít nhất 10 ký tự."); return; }
    onConfirm({
      cancelReason: selectedReason,
      cancelReasonNote: isOther ? note.trim() : undefined,
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4 backdrop-blur-sm">
      <div className="w-full max-w-sm rounded-panel border border-white/70 bg-white/95 p-6 shadow-2xl backdrop-blur">
        <h2 className="font-fredoka text-xl font-bold text-black-blue">Huỷ đơn hàng</h2>
        <p className="mt-1 text-sm text-ink/60">Vui lòng cho biết lý do huỷ đơn.</p>

        <form className="mt-4 space-y-3" onSubmit={handleSubmit}>
          <div className="space-y-2">
            {CUSTOMER_CANCEL_REASONS.map((reason) => (
              <label
                key={reason}
                className={[
                  "flex cursor-pointer items-center gap-3 rounded-xl border px-4 py-2.5 text-sm transition",
                  selectedReason === reason
                    ? "border-aqua bg-aqua/10 font-semibold text-black-blue"
                    : "border-white/60 bg-white/50 text-black-blue hover:border-aqua/50",
                ].join(" ")}
              >
                <input
                  checked={selectedReason === reason}
                  className="sr-only"
                  name="reason"
                  onChange={() => { setSelectedReason(reason); if (reason !== "Lý do khác") setNote(""); setError(null); }}
                  type="radio"
                  value={reason}
                />
                {reason}
              </label>
            ))}
          </div>

          {isOther && (
            <div>
              <textarea
                autoFocus
                className="mt-1 h-20 w-full resize-none rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-none focus:ring-2 focus:ring-aqua"
                maxLength={1000}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Nhập lý do (tối thiểu 10 ký tự)..."
                value={note}
              />
              <p className="mt-1 text-right text-xs text-ink/40">{note.length}/1000</p>
            </div>
          )}

          {error && (
            <p className="rounded-xl border border-peach/50 bg-peach/10 px-3 py-2 text-sm text-black-blue">{error}</p>
          )}

          <div className="flex gap-2 pt-1">
            <button
              className="flex-1 rounded-full border border-peach/60 bg-peach/20 py-2.5 text-sm font-bold text-black-blue transition hover:bg-peach/40 disabled:opacity-50"
              disabled={isLoading}
              type="submit"
            >
              {isLoading ? "Đang huỷ..." : "Xác nhận huỷ"}
            </button>
            <button
              className="flex-1 rounded-full border border-white/60 bg-white/60 py-2.5 text-sm font-semibold text-black-blue transition hover:bg-white"
              disabled={isLoading}
              onClick={onClose}
              type="button"
            >
              Quay lại
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function OrdersPage() {
  const user = useAuthStore((s) => s.user);
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeStatus, setActiveStatus] = useState("");
  const [cancelTarget, setCancelTarget] = useState<string | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);

  useEffect(() => {
    if (!user || user.role !== "CUSTOMER") return;

    let active = true;
    setIsLoading(true);

    getCustomerOrders({ status: activeStatus || undefined, size: 50 })
      .then((res) => {
        if (active) setOrders(res.data ?? []);
      })
      .catch(() => {
        if (active) toast.error("Không thể tải danh sách đơn hàng.");
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => { active = false; };
  }, [user, activeStatus]);

  if (!user) {
    return <Navigate replace state={{ from: "/orders" }} to="/auth" />;
  }

  if (user.role !== "CUSTOMER") {
    return <Navigate replace to="/" />;
  }

  const handleCancel = async (req: CancelOrderRequest) => {
    if (!cancelTarget) return;
    setIsCancelling(true);
    try {
      const res = await cancelCustomerOrder(cancelTarget, req);
      // Filtered tab: remove immediately so the order leaves the non-CANCELLED view.
      // "Tất cả" tab: update in-place to show the CANCELLED badge.
      setOrders((prev) =>
        activeStatus
          ? prev.filter((o) => o.id !== cancelTarget)
          : prev.map((o) => (o.id === cancelTarget ? res.data : o)),
      );
      setCancelTarget(null);
      toast.success("Đã huỷ đơn hàng.");
      // Silently re-sync
      getCustomerOrders({ status: activeStatus || undefined, size: 50 })
        .then((r) => setOrders(r.data ?? []))
        .catch(() => { /* non-critical */ });
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <div className="relative z-10 mx-auto max-w-4xl">
        <div className="mb-6 flex items-center gap-4">
          <h1 className="font-fredoka text-4xl font-bold text-black-blue">Đơn hàng của tôi</h1>
        </div>

        <div className="mb-5 flex flex-wrap gap-2">
          {STATUS_TABS.map((tab) => (
            <button
              className={[
                "rounded-full border px-4 py-1.5 text-sm font-semibold transition",
                activeStatus === tab.value
                  ? "border-black-blue bg-black-blue text-white"
                  : "border-white/60 bg-white/50 text-black-blue hover:border-black-blue",
              ].join(" ")}
              key={tab.value}
              onClick={() => setActiveStatus(tab.value)}
              type="button"
            >
              {tab.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="py-20 text-center text-sm text-ink/60">Đang tải đơn hàng...</div>
        ) : orders.length === 0 ? (
          <div className="flex min-h-[40vh] flex-col items-center justify-center rounded-panel border border-white/50 bg-white/35 px-8 py-16 text-center shadow-glass backdrop-blur-xl">
            <p className="font-fredoka text-5xl">📦</p>
            <h2 className="mt-4 font-fredoka text-2xl font-bold text-black-blue">Chưa có đơn hàng nào</h2>
            <Link
              className="mt-6 rounded-full bg-black-blue px-8 py-3 text-sm font-bold text-white transition hover:-translate-y-0.5 hover:bg-ink"
              to="/merch"
            >
              Mua sắm ngay
            </Link>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            {orders.map((order) => (
              <div
                className="rounded-panel border border-white/50 bg-white/45 p-5 shadow-glass backdrop-blur-xl"
                key={order.id}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase text-ink/50">
                      Đơn hàng #{order.id.slice(0, 8).toUpperCase()}
                    </p>
                    <p className="mt-1 text-xs text-ink/50">{formatDate(order.createdAt)}</p>
                    {order.status === "READY" && order.pickupSchedule && (
                      <p className="mt-1 text-xs font-semibold text-green-700">
                        📍 {order.pickupSchedule.pickupDate
                          ? new Date(order.pickupSchedule.pickupDate).toLocaleDateString("vi-VN")
                          : ""}{" "}
                        · {order.pickupSchedule.pickupTimeSlot}
                      </p>
                    )}
                  </div>
                  <StatusBadge status={order.status} />
                </div>

                <div className="mt-3 border-t border-white/40 pt-3">
                  {order.items && order.items.length > 0 ? (
                    <div className="space-y-1">
                      {order.items.map((item) => (
                        <div className="flex justify-between text-sm" key={item.id}>
                          <span className="text-ink/70">{item.merchName} × {item.quantity}</span>
                          <span className="font-semibold text-black-blue">{formatPrice(item.subtotal)}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-ink/50">{order.items?.length ?? 0} sản phẩm</p>
                  )}
                </div>

                {order.status === "CANCELLED" && order.cancelReason && (
                  <div className="mt-3 rounded-xl border border-peach/30 bg-peach/10 px-3 py-2 text-xs text-ink/60">
                    Đã huỷ: {order.cancelReason}
                  </div>
                )}

                <div className="mt-4 flex items-center justify-between border-t border-white/40 pt-4">
                  <p className="font-fredoka text-xl font-bold text-black-blue">{formatPrice(order.totalAmount)}</p>
                  <div className="flex gap-2">
                    {order.status === "PENDING" && (
                      <button
                        className="rounded-full border border-peach/60 bg-peach/20 px-4 py-1.5 text-xs font-semibold text-black-blue transition hover:bg-peach/40"
                        onClick={() => setCancelTarget(order.id)}
                        type="button"
                      >
                        Huỷ đơn
                      </button>
                    )}
                    <Link
                      className="rounded-full border border-white/60 bg-white/60 px-4 py-1.5 text-xs font-semibold text-black-blue transition hover:border-aqua hover:bg-white"
                      to={`/orders/${order.id}`}
                    >
                      Chi tiết →
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {cancelTarget && (
        <CancelOrderModal
          isLoading={isCancelling}
          onClose={() => setCancelTarget(null)}
          onConfirm={handleCancel}
        />
      )}
    </main>
  );
}
