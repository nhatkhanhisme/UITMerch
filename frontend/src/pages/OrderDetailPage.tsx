import { lazy, Suspense, useEffect, useState } from "react";
import { Link, Navigate, useParams } from "react-router-dom";
import { getApiErrorMessage } from "../api/auth";
import { cancelCustomerOrder, getCustomerOrder } from "../api/order";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { OrderResponse } from "../types/shared";

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
  READY_FOR_PICKUP: "Sẵn sàng nhận",
  SUCCESS: "Hoàn thành",
  CANCELLED: "Đã huỷ",
};

const PAYMENT_METHOD_LABELS: Record<string, string> = {
  CASH_ON_DELIVERY: "Thanh toán khi nhận hàng (COD)",
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
  READY_FOR_PICKUP: "bg-green-100 text-green-800 border-green-200",
  SUCCESS: "bg-green-200 text-green-900 border-green-300",
  CANCELLED: "bg-peach/20 text-black-blue border-peach/40",
};

const STATUS_STEPS = ["PENDING", "CONFIRMED", "READY_FOR_PICKUP", "SUCCESS"];

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
    <div className="flex items-center gap-1">
      {STATUS_STEPS.map((step, idx) => (
        <div className="flex flex-1 flex-col items-center" key={step}>
          <div
            className={[
              "flex size-7 items-center justify-center rounded-full border-2 text-xs font-bold",
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
                "absolute left-1/2 h-0.5 w-full translate-y-3",
                idx < currentIdx ? "bg-aqua" : "bg-white/30",
              ].join(" ")}
            />
          )}
        </div>
      ))}
    </div>
  );
}

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const user = useAuthStore((s) => s.user);

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCancelling, setIsCancelling] = useState(false);

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

  const handleCancel = async () => {
    if (!order || !confirm("Bạn có chắc muốn huỷ đơn hàng này?")) return;
    setIsCancelling(true);
    try {
      const res = await cancelCustomerOrder(order.id);
      setOrder(res.data);
      toast.success("Đã huỷ đơn hàng.");
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
          <div className="py-20 text-center text-sm text-ink/60">
            Đang tải đơn hàng...
          </div>
        ) : !order ? (
          <div className="rounded-panel border border-white/50 bg-white/35 p-8 text-center shadow-glass backdrop-blur-xl">
            <p className="font-fredoka text-2xl font-bold text-black-blue">
              Không tìm thấy đơn hàng
            </p>
            <Link
              className="mt-6 inline-block rounded-full bg-black-blue px-8 py-3 text-sm font-bold text-white"
              to="/orders"
            >
              Xem danh sách đơn
            </Link>
          </div>
        ) : (
          <div className="rounded-panel border border-white/50 bg-white/45 shadow-glass backdrop-blur-xl">
            <div className="p-6 sm:p-8">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="font-sans text-xs font-semibold uppercase tracking-widest text-ink/50">
                    Đơn hàng
                  </p>
                  <h1 className="mt-1 font-fredoka text-3xl font-bold text-black-blue">
                    #{order.id.slice(0, 8).toUpperCase()}
                  </h1>
                  <p className="mt-1 text-xs text-ink/50">
                    Ngày tạo: {formatDateTime(order.createdAt)}
                  </p>
                </div>
                <span
                  className={[
                    "inline-flex items-center rounded-full border px-4 py-1 text-sm font-semibold",
                    STATUS_COLORS[order.status] ||
                      "bg-white/40 text-ink border-white/40",
                  ].join(" ")}
                >
                  {STATUS_LABELS[order.status] || order.status}
                </span>
              </div>

              <div className="mt-6">
                <OrderProgressBar status={order.status} />
              </div>
            </div>

            <div className="border-t border-white/40 p-6 sm:p-8">
              <h2 className="font-fredoka text-xl font-bold text-black-blue">
                Sản phẩm đặt mua
              </h2>
              <div className="mt-4 space-y-3">
                {(order.items ?? []).map((item) => (
                  <div
                    className="flex justify-between rounded-xl border border-white/40 bg-white/30 px-4 py-3 text-sm"
                    key={item.id}
                  >
                    <div>
                      <p className="font-semibold text-black-blue">
                        {item.merchName}
                      </p>
                      <p className="text-xs text-ink/55">
                        {formatPrice(item.unitPrice)} × {item.quantity}
                      </p>
                    </div>
                    <p className="font-bold text-black-blue">
                      {formatPrice(item.subtotal)}
                    </p>
                  </div>
                ))}
              </div>

              <div className="mt-4 flex justify-between border-t border-white/40 pt-4 text-sm font-bold text-black-blue">
                <span>Tổng cộng</span>
                <span className="font-fredoka text-xl">
                  {formatPrice(order.totalAmount)}
                </span>
              </div>
            </div>

            <div className="border-t border-white/40 p-6 sm:p-8">
              <h2 className="font-fredoka text-xl font-bold text-black-blue">
                Thông tin giao hàng
              </h2>
              <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
                {order.guestName && (
                  <div>
                    <p className="text-xs font-semibold text-ink/50">
                      Người nhận
                    </p>
                    <p className="text-black-blue">{order.guestName}</p>
                  </div>
                )}
                {order.guestPhone && (
                  <div>
                    <p className="text-xs font-semibold text-ink/50">
                      Số điện thoại
                    </p>
                    <p className="text-black-blue">{order.guestPhone}</p>
                  </div>
                )}
                {order.guestAddress && (
                  <div className="sm:col-span-2">
                    <p className="text-xs font-semibold text-ink/50">
                      Địa chỉ
                    </p>
                    <p className="text-black-blue">{order.guestAddress}</p>
                  </div>
                )}
                {order.note && (
                  <div className="sm:col-span-2">
                    <p className="text-xs font-semibold text-ink/50">Ghi chú</p>
                    <p className="text-black-blue">{order.note}</p>
                  </div>
                )}
                <div>
                  <p className="text-xs font-semibold text-ink/50">
                    Thanh toán
                  </p>
                  <p className="text-black-blue">
                    {PAYMENT_METHOD_LABELS[order.paymentMethod] ?? order.paymentMethod}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-semibold text-ink/50">
                    Trạng thái thanh toán
                  </p>
                  <p className="text-black-blue">
                    {PAYMENT_STATUS_LABELS[order.paymentStatus] ?? order.paymentStatus}
                  </p>
                </div>
              </div>
            </div>

            {order.status === "PENDING" && (
              <div className="border-t border-white/40 p-6 sm:p-8">
                <button
                  className="rounded-full border border-peach/60 bg-peach/20 px-6 py-2.5 text-sm font-semibold text-black-blue transition hover:bg-peach/40 disabled:opacity-50"
                  disabled={isCancelling}
                  onClick={handleCancel}
                  type="button"
                >
                  {isCancelling ? "Đang huỷ..." : "Huỷ đơn hàng"}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </main>
  );
}
