import { CustomerPickup, OrderHistoryPanel } from "../features/Pickup";
import { CampaignOrderNotice } from "../features/Campaigns";
import { useEffect, useState } from "react";
import {
  ArrowLeft,
  CalendarDays,
  Clock3,
  MapPin,
  Copy,
  Check,
  Package,
} from "lucide-react";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { Link, Navigate, useParams } from "react-router-dom";
import { getApiErrorMessage } from "../api/auth";
import { cancelCustomerOrder, getCustomerOrder } from "../api/order";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { CancelOrderRequest, OrderResponse } from "../types/shared";

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
    <ol className="order-progress" aria-label="Tiến trình đơn hàng">
      {STATUS_STEPS.map((step, idx) => (
        <li
          className="relative flex flex-1 flex-col items-center"
          key={step}
          aria-current={idx === currentIdx ? "step" : undefined}
        >
          <div
            className={[
              "flex size-9 items-center justify-center rounded-full border-2 text-sm font-bold z-10",
              idx <= currentIdx
                ? "border-cyan-700 bg-cyan-700 text-white"
                : "border-gray/25 bg-white text-gray",
            ].join(" ")}
          >
            {idx < currentIdx ? (
              <Check aria-hidden="true" size={16} />
            ) : (
              idx + 1
            )}
          </div>
          <p className="mt-2 text-center text-xs font-medium text-slate leading-snug sm:text-sm">
            {STATUS_LABELS[step]}
          </p>
          {idx < STATUS_STEPS.length - 1 && (
            <div
              className={[
                "absolute top-[18px] left-1/2 h-0.5 w-full",
                idx < currentIdx ? "bg-cyan-700" : "bg-gray/20",
              ].join(" ")}
            />
          )}
        </li>
      ))}
    </ol>
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
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    const handler = () => setRefreshTrigger((t) => t + 1);
    window.addEventListener("order-status-changed", handler);
    return () => window.removeEventListener("order-status-changed", handler);
  }, []);

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
  }, [id, user, refreshTrigger]);

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
    <main className="customer-order-page">
      <AmbientBackgroundGradients />
      <div className="customer-order-shell">
        <Link className="order-back-link" to="/orders">
          <ArrowLeft aria-hidden="true" size={18} /> Đơn hàng của tôi
        </Link>
        {isLoading ? (
          <div
            className="order-card py-16 text-center text-slate"
            role="status"
          >
            Đang tải đơn hàng…
          </div>
        ) : !order ? (
          <section className="order-card text-center">
            <h1 className="text-2xl font-bold">Không tìm thấy đơn hàng</h1>
            <Link className="feature-button mt-6 inline-flex" to="/orders">
              Xem danh sách đơn
            </Link>
          </section>
        ) : (
          <>
            <header className="order-card order-overview">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0">
                  <p className="order-eyebrow">Đơn hàng · Nhận tại trường</p>
                  <div className="mt-2 flex flex-wrap items-center gap-3">
                    <h1 className="text-2xl font-bold tracking-tight text-black-blue sm:text-3xl">
                      #{order.id.slice(0, 8).toUpperCase()}
                    </h1>
                    <button
                      className="order-copy"
                      onClick={handleCopyCode}
                      title="Sao chép mã đơn đầy đủ"
                      type="button"
                    >
                      {copiedCode ? (
                        <Check aria-hidden="true" size={16} />
                      ) : (
                        <Copy aria-hidden="true" size={16} />
                      )}
                      {copiedCode ? "Đã sao chép!" : "Sao chép"}
                    </button>
                  </div>
                  <p className="mt-2 text-sm text-slate">
                    Ngày tạo: {formatDateTime(order.createdAt)}
                  </p>
                  <details className="order-reference">
                    <summary>Xem mã đơn đầy đủ</summary>
                    <code>{order.id}</code>
                  </details>
                </div>
                <span
                  className={[
                    "order-status",
                    STATUS_COLORS[order.status] ||
                      "border-gray/20 bg-white text-slate",
                  ].join(" ")}
                >
                  {STATUS_LABELS[order.status] || order.status}
                </span>
              </div>
              <OrderProgressBar status={order.status} />
              <CampaignOrderNotice orderId={order.id} />
            </header>
            <div className="customer-order-layout">
              <aside
                className="order-pickup-column"
                aria-label="Thông tin nhận hàng"
              >
                <section className="order-card pickup-schedule">
                  <div className="panel-heading">
                    <CalendarDays aria-hidden="true" size={22} />
                    <h2>Lịch nhận hàng</h2>
                  </div>
                  {order.pickupSchedule ? (
                    <>
                      <dl className="pickup-schedule-details">
                        <div>
                          <CalendarDays aria-hidden="true" size={18} />
                          <div>
                            <dt>Ngày nhận</dt>
                            <dd>
                              {new Date(
                                order.pickupSchedule.pickupDate,
                              ).toLocaleDateString("vi-VN", {
                                weekday: "long",
                                year: "numeric",
                                month: "long",
                                day: "numeric",
                              })}
                            </dd>
                          </div>
                        </div>
                        <div>
                          <Clock3 aria-hidden="true" size={18} />
                          <div>
                            <dt>Khung giờ</dt>
                            <dd>{order.pickupSchedule.pickupTimeSlot}</dd>
                          </div>
                        </div>
                        <div>
                          <MapPin aria-hidden="true" size={18} />
                          <div>
                            <dt>Địa điểm</dt>
                            <dd>{order.pickupSchedule.location}</dd>
                          </div>
                        </div>
                      </dl>
                      {order.pickupSchedule.notes && (
                        <p className="pickup-schedule-note">
                          {order.pickupSchedule.notes}
                        </p>
                      )}
                      <p className="pickup-help">
                        Mã đơn:{" "}
                        <strong>#{order.id.slice(0, 8).toUpperCase()}</strong>.{" "}
                        {order.status === "READY"
                          ? "Chuẩn bị mã QR bên dưới khi nhận hàng."
                          : order.status === "COMPLETED"
                            ? "Bạn đã nhận hàng."
                            : order.status === "CANCELLED"
                              ? "Đơn hàng đã được hủy."
                              : "Mã QR sẽ xuất hiện khi đơn hàng sẵn sàng nhận."}
                      </p>
                    </>
                  ) : (
                    <p className="panel-description">
                      Chưa có lịch nhận hàng riêng. Liên hệ tổ chức để biết thời
                      gian và địa điểm nhận hàng.
                    </p>
                  )}
                </section>
                {order.status === "READY" && (
                  <CustomerPickup key={order.id} orderId={order.id} />
                )}
              </aside>
              <div className="order-main-column">
                <section
                  className="order-card"
                  aria-labelledby="order-items-title"
                >
                  <div className="panel-heading">
                    <Package aria-hidden="true" size={22} />
                    <h2 id="order-items-title">Sản phẩm đặt mua</h2>
                  </div>
                  <div className="order-items">
                    {(order.items ?? []).map((item) => (
                      <div className="order-item" key={item.id}>
                        <div className="min-w-0">
                          <p className="font-semibold text-black-blue break-words">
                            {item.merchName}
                          </p>
                          <p className="mt-1 text-sm text-slate">
                            {formatPrice(item.unitPrice)} × {item.quantity}
                          </p>
                        </div>
                        <p className="font-semibold text-black-blue">
                          {formatPrice(item.subtotal)}
                        </p>
                      </div>
                    ))}
                  </div>
                  <div className="order-total">
                    <span>Tổng cộng</span>
                    <strong>{formatPrice(order.totalAmount)}</strong>
                  </div>
                </section>
                <section
                  className="order-card"
                  aria-labelledby="order-contact-title"
                >
                  <h2 id="order-contact-title">Thông tin đơn hàng</h2>
                  <dl className="order-info-grid">
                    <div>
                      <dt>Người đặt</dt>
                      <dd>{order.guestName || user.fullName}</dd>
                    </div>
                    {order.guestPhone && (
                      <div>
                        <dt>Số điện thoại</dt>
                        <dd>{order.guestPhone}</dd>
                      </div>
                    )}
                    <div>
                      <dt>Phương thức thanh toán</dt>
                      <dd>
                        {order.paymentMethod === "CASH_ON_DELIVERY"
                          ? "Thanh toán khi nhận hàng"
                          : (PAYMENT_METHOD_LABELS[order.paymentMethod] ??
                            order.paymentMethod)}
                      </dd>
                    </div>
                    <div>
                      <dt>Trạng thái thanh toán</dt>
                      <dd>
                        {PAYMENT_STATUS_LABELS[order.paymentStatus] ??
                          order.paymentStatus}
                      </dd>
                    </div>
                    {order.note && (
                      <div className="sm:col-span-2">
                        <dt>Ghi chú</dt>
                        <dd>{order.note}</dd>
                      </div>
                    )}
                  </dl>
                  {order.status === "PENDING" && (
                    <div className="order-cancel-action">
                      <button
                        onClick={() => setShowCancelModal(true)}
                        type="button"
                      >
                        Huỷ đơn hàng
                      </button>
                    </div>
                  )}
                </section>
                {order.status === "CANCELLED" && order.cancelReason && (
                  <section className="order-card border-orange-200 bg-orange-50">
                    <h2>Lý do huỷ</h2>
                    <p className="mt-3 font-semibold">{order.cancelReason}</p>
                    {order.cancelReasonNote && (
                      <p className="mt-2 text-slate">
                        {order.cancelReasonNote}
                      </p>
                    )}
                    <p className="mt-3 text-sm text-slate">
                      Huỷ bởi:{" "}
                      {order.cancelledBy === "customer"
                        ? "Khách hàng"
                        : String(order.cancelledBy) === "campaign"
                          ? "Hệ thống đặt trước"
                          : "Ban tổ chức"}
                      {order.cancelledAt
                        ? ` · ${formatDateTime(order.cancelledAt)}`
                        : ""}
                    </p>
                  </section>
                )}
                <OrderHistoryPanel key={order.id} orderId={order.id} />
              </div>
            </div>
          </>
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
