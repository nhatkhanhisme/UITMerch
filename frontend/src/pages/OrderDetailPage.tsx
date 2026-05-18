import { lazy, Suspense, useEffect, useState } from "react";
import { Link, Navigate, useParams } from "react-router-dom";
import { getApiErrorMessage } from "../api/auth";
import { cancelCustomerOrder, getCustomerOrder } from "../api/order";
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

function formatDateTime(dateStr?: string) {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleString("vi-VN");
}

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  READY: "Sẵn sàng nhận",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã huỷ",
};

const PAYMENT_METHOD_LABELS: Record<string, string> = {
  CASH_ON_DELIVERY: "Nhận hàng tại trường",
};

const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ xử lý",
  PAID: "Đã thanh toán",
  FAILED: "Thất bại",
  REFUNDED: "Đã hoàn tiền",
};

const STATUS_COLORS: Record<string, string> = {
  PENDING: "bg-gold/20 text-black-blue border-gold/40",
  CONFIRMED: "bg-aqua/20 text-black-blue border-aqua/40",
  READY: "bg-green-100 text-green-800 border-green-200",
  COMPLETED: "bg-green-200 text-green-900 border-green-300",
  CANCELLED: "bg-peach/20 text-black-blue border-peach/40",
};

const STATUS_STEPS = ["PENDING", "CONFIRMED", "READY", "COMPLETED"];

function OrderProgressBar({ status }: { status: string }) {
  if (status === "CANCELLED") {
    return (
      <div className="rounded-xl border border-peach/40 bg-peach/10 px-4 py-3 text-sm font-semibold text-black-blue">
        Đơn hàng đã bị huỷ
      </div>
    );
  }
  const currentIdx = STATUS_STEPS.indexOf(status);
  return (
    <div className="relative flex items-start gap-1">
      {STATUS_STEPS.map((step, idx) => (
        <div className="relative flex flex-1 flex-col items-center" key={step}>
          <div
            className={[
              "flex size-7 items-center justify-center rounded-full border-2 text-xs font-bold z-10",
              idx <= currentIdx
                ? "border-aqua bg-aqua text-black-blue"
                : "border-white/50 bg-white/30 text-ink/40",
            ].join(" ")}
          >
            {idx < currentIdx ? "✓" : idx + 1}
          </div>
          <p className="mt-1 text-center text-[10px] text-ink/60 leading-tight">
            {STATUS_LABELS[step]}
          </p>
          {idx < STATUS_STEPS.length - 1 && (
            <div
              className={[
                "absolute top-3.5 left-1/2 h-0.5 w-full",
                idx < currentIdx ? "bg-aqua" : "bg-white/30",
              ].join(" ")}
            />
          )}
        </div>
      ))}
    </div>
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
    if (!selectedReason) {
      setError("Vui lòng chọn lý do huỷ đơn.");
      return;
    }
    if (isOther && note.trim().length < 10) {
      setError("Lý do khác phải có ít nhất 10 ký tự.");
      return;
    }
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
                  onChange={() => {
                    setSelectedReason(reason);
                    if (reason !== "Lý do khác") setNote("");
                    setError(null);
                  }}
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
            <p className="rounded-xl border border-peach/50 bg-peach/10 px-3 py-2 text-sm text-black-blue">
              {error}
            </p>
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

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const user = useAuthStore((s) => s.user);

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);

  useEffect(() => {
    if (!id || !user || user.role !== "CUSTOMER") return;

    let active = true;
    setIsLoading(true);

    getCustomerOrder(id)
      .then((res) => {
        if (active) setOrder(res.data);
      })
      .catch(() => {
        if (active) toast.error("Không thể tải thông tin đơn hàng.");
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [id, user]);

  if (!user) {
    return <Navigate replace state={{ from: `/orders/${id}` }} to="/auth" />;
  }

  if (user.role !== "CUSTOMER") {
    return <Navigate replace to="/" />;
  }

  const handleCancel = async (req: CancelOrderRequest) => {
    if (!order) return;
    setIsCancelling(true);
    try {
      const res = await cancelCustomerOrder(order.id, req);
      setOrder(res.data);
      setShowCancelModal(false);
      toast.success("Đã huỷ đơn hàng.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setIsCancelling(false);
    }
  };

  const handleCopyCode = () => {
    if (!order) return;
    navigator.clipboard.writeText(order.id).then(() => {
      setCopiedCode(true);
      setTimeout(() => setCopiedCode(false), 2000);
    });
  };

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <div className="relative z-10 mx-auto max-w-3xl">
        <div className="mb-6 flex items-center gap-3">
          <Link
            className="inline-flex items-center rounded-full border border-white/70 bg-white/65 px-5 py-2.5 text-sm font-bold text-black-blue shadow-glass-inset transition hover:-translate-y-0.5 hover:border-aqua hover:bg-white"
            to="/orders"
          >
            ← Đơn hàng của tôi
          </Link>
        </div>

        {isLoading ? (
          <div className="py-20 text-center text-sm text-ink/60">Đang tải đơn hàng...</div>
        ) : !order ? (
          <div className="rounded-panel border border-white/50 bg-white/35 p-8 text-center shadow-glass backdrop-blur-xl">
            <p className="font-fredoka text-2xl font-bold text-black-blue">Không tìm thấy đơn hàng</p>
            <Link
              className="mt-6 inline-block rounded-full bg-black-blue px-8 py-3 text-sm font-bold text-white"
              to="/orders"
            >
              Xem danh sách đơn
            </Link>
          </div>
        ) : (
          <div className="rounded-panel border border-white/50 bg-white/45 shadow-glass backdrop-blur-xl">

            {/* Header — order code + status */}
            <div className="p-6 sm:p-8">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="inline-flex items-center rounded-full border border-aqua/50 bg-aqua/10 px-2.5 py-0.5 text-xs font-bold uppercase tracking-widest text-black-blue">
                      Pre-order · Nhận tại trường
                    </span>
                  </div>
                  <p className="mt-2 font-sans text-xs font-semibold uppercase tracking-widest text-ink/50">
                    Mã đơn hàng
                  </p>
                  <div className="mt-1 flex items-center gap-2">
                    <h1 className="font-fredoka text-3xl font-bold text-black-blue tracking-wide">
                      #{order.id.slice(0, 8).toUpperCase()}
                    </h1>
                    <button
                      className="rounded-full border border-white/60 bg-white/60 px-3 py-1 text-xs font-semibold text-black-blue transition hover:border-aqua hover:bg-white"
                      onClick={handleCopyCode}
                      title="Sao chép mã đơn đầy đủ"
                      type="button"
                    >
                      {copiedCode ? "Đã sao chép!" : "Sao chép"}
                    </button>
                  </div>
                  <p className="mt-1 font-mono text-xs text-ink/35 select-all">{order.id}</p>
                  <p className="mt-1 text-xs text-ink/50">Ngày tạo: {formatDateTime(order.createdAt)}</p>
                </div>
                <span
                  className={[
                    "inline-flex items-center rounded-full border px-4 py-1 text-sm font-semibold",
                    STATUS_COLORS[order.status] || "bg-white/40 text-ink border-white/40",
                  ].join(" ")}
                >
                  {STATUS_LABELS[order.status] || order.status}
                </span>
              </div>

              <div className="mt-6">
                <OrderProgressBar status={order.status} />
              </div>
            </div>

            {/* Pickup schedule info */}
            {order.pickupSchedule && (
              <div className="border-t border-white/40 p-6 sm:p-8">
                <h2 className="font-fredoka text-xl font-bold text-black-blue">Lịch nhận hàng</h2>
                <div className="mt-3 rounded-xl border border-aqua/40 bg-aqua/10 p-4 space-y-2 text-sm">
                  <div className="flex items-center gap-2">
                    <span className="text-base">📅</span>
                    <span className="font-semibold text-black-blue">
                      {new Date(order.pickupSchedule.pickupDate).toLocaleDateString("vi-VN", {
                        weekday: "long", year: "numeric", month: "long", day: "numeric",
                      })}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-base">🕐</span>
                    <span className="text-black-blue">{order.pickupSchedule.pickupTimeSlot}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-base">📍</span>
                    <span className="text-black-blue">{order.pickupSchedule.location}</span>
                  </div>
                  {order.pickupSchedule.notes && (
                    <div className="flex items-start gap-2">
                      <span className="text-base">📝</span>
                      <span className="text-ink/70">{order.pickupSchedule.notes}</span>
                    </div>
                  )}
                  <p className="mt-2 rounded-lg border border-aqua/30 bg-white/60 px-3 py-2 text-xs font-semibold text-black-blue">
                    Vui lòng xuất trình mã đơn hàng khi đến nhận: <span className="font-mono font-bold">{order.id.slice(0, 8).toUpperCase()}</span>
                  </p>
                </div>
              </div>
            )}

            {/* Cancel reason */}
            {order.status === "CANCELLED" && order.cancelReason && (
              <div className="border-t border-white/40 p-6 sm:p-8">
                <h2 className="font-fredoka text-xl font-bold text-black-blue">Lý do huỷ</h2>
                <div className="mt-3 rounded-xl border border-peach/40 bg-peach/10 p-4 text-sm">
                  <p className="font-semibold text-black-blue">{order.cancelReason}</p>
                  {order.cancelReasonNote && (
                    <p className="mt-1 text-ink/60">{order.cancelReasonNote}</p>
                  )}
                  <p className="mt-2 text-xs text-ink/40">
                    Huỷ bởi: {order.cancelledBy === "customer" ? "Khách hàng" : "Ban tổ chức"}
                    {order.cancelledAt ? ` · ${formatDateTime(order.cancelledAt)}` : ""}
                  </p>
                </div>
              </div>
            )}

            {/* Items */}
            <div className="border-t border-white/40 p-6 sm:p-8">
              <h2 className="font-fredoka text-xl font-bold text-black-blue">Sản phẩm đặt mua</h2>
              <div className="mt-4 space-y-3">
                {(order.items ?? []).map((item) => (
                  <div
                    className="flex justify-between rounded-xl border border-white/40 bg-white/30 px-4 py-3 text-sm"
                    key={item.id}
                  >
                    <div>
                      <p className="font-semibold text-black-blue">{item.merchName}</p>
                      <p className="text-xs text-ink/55">
                        {formatPrice(item.unitPrice)} × {item.quantity}
                      </p>
                    </div>
                    <p className="font-bold text-black-blue">{formatPrice(item.subtotal)}</p>
                  </div>
                ))}
              </div>
              <div className="mt-4 flex justify-between border-t border-white/40 pt-4 text-sm font-bold text-black-blue">
                <span>Tổng cộng</span>
                <span className="font-fredoka text-xl">{formatPrice(order.totalAmount)}</span>
              </div>
            </div>

            {/* Order info */}
            <div className="border-t border-white/40 p-6 sm:p-8">
              <h2 className="font-fredoka text-xl font-bold text-black-blue">Thông tin đơn</h2>
              <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
                {order.guestName && (
                  <div>
                    <p className="text-xs font-semibold text-ink/50">Người đặt</p>
                    <p className="text-black-blue">{order.guestName}</p>
                  </div>
                )}
                {order.guestPhone && (
                  <div>
                    <p className="text-xs font-semibold text-ink/50">Số điện thoại</p>
                    <p className="text-black-blue">{order.guestPhone}</p>
                  </div>
                )}
                {order.note && (
                  <div className="sm:col-span-2">
                    <p className="text-xs font-semibold text-ink/50">Ghi chú</p>
                    <p className="text-black-blue">{order.note}</p>
                  </div>
                )}
                <div>
                  <p className="text-xs font-semibold text-ink/50">Thanh toán</p>
                  <p className="text-black-blue">
                    {PAYMENT_METHOD_LABELS[order.paymentMethod] ?? order.paymentMethod}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-semibold text-ink/50">Trạng thái thanh toán</p>
                  <p className="text-black-blue">
                    {PAYMENT_STATUS_LABELS[order.paymentStatus] ?? order.paymentStatus}
                  </p>
                </div>
              </div>
            </div>

            {/* Cancel button */}
            {order.status === "PENDING" && (
              <div className="border-t border-white/40 p-6 sm:p-8">
                <button
                  className="rounded-full border border-peach/60 bg-peach/20 px-6 py-2.5 text-sm font-semibold text-black-blue transition hover:bg-peach/40"
                  onClick={() => setShowCancelModal(true)}
                  type="button"
                >
                  Huỷ đơn hàng
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {showCancelModal && (
        <CancelOrderModal
          isLoading={isCancelling}
          onClose={() => setShowCancelModal(false)}
          onConfirm={handleCancel}
        />
      )}
    </main>
  );
}
