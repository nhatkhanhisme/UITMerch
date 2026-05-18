import { lazy, Suspense, useEffect, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import {
  checkoutCart,
  getCart,
  removeCartItem,
  updateCartItem,
} from "../api/cart";
import { getApiErrorMessage } from "../api/auth";
import { getCustomerProfile } from "../api/profile";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { CartResponse, OrderResponse } from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency",
});

function formatPrice(price?: number | null) {
  if (price == null) return "Miễn phí";
  return currencyFormatter.format(price);
}

function EmptyCart() {
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center rounded-panel border border-white/50 bg-white/35 px-8 py-16 text-center shadow-glass backdrop-blur-xl">
      <p className="font-fredoka text-6xl">🛒</p>
      <h2 className="mt-4 font-fredoka text-3xl font-bold text-black-blue">
        Giỏ hàng trống
      </h2>
      <p className="mt-3 text-sm leading-7 text-ink/65">
        Bạn chưa thêm vật phẩm nào vào giỏ. Khám phá kho hàng để bắt đầu!
      </p>
      <Link
        className="mt-8 rounded-full bg-black-blue px-8 py-3 text-sm font-bold text-white transition hover:-translate-y-0.5 hover:bg-ink"
        to="/merch"
      >
        Khám phá vật phẩm
      </Link>
    </div>
  );
}

function OrderSuccess({
  orders,
  onContinue,
}: {
  orders: OrderResponse[];
  onContinue: () => void;
}) {
  return (
    <div className="flex flex-col items-center rounded-panel border border-aqua/60 bg-white/70 px-8 py-16 text-center shadow-glass backdrop-blur-xl">
      <div className="flex size-20 items-center justify-center rounded-full bg-aqua/20 text-4xl text-black-blue">
        ✓
      </div>
      <h2 className="mt-6 font-fredoka text-4xl font-bold text-black-blue">
        Đặt trước thành công!
      </h2>
      <p className="mt-3 text-sm leading-7 text-ink/70">
        {orders.length === 1
          ? "Đơn hàng của bạn đã được tạo."
          : `${orders.length} đơn hàng đã được tạo (mỗi tổ chức một đơn).`}
      </p>
      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <Link
          className="rounded-full bg-black-blue px-8 py-3 text-sm font-bold text-white transition hover:-translate-y-0.5 hover:bg-ink"
          to="/orders"
        >
          Xem đơn hàng
        </Link>
        <button
          className="rounded-full border border-ink/20 bg-white/60 px-8 py-3 text-sm font-bold text-black-blue transition hover:-translate-y-0.5 hover:bg-white"
          onClick={onContinue}
          type="button"
        >
          Tiếp tục mua sắm
        </button>
      </div>
    </div>
  );
}

export function CartPage() {
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [updatingItemId, setUpdatingItemId] = useState<string | null>(null);
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const [checkoutNote, setCheckoutNote] = useState("");
  const [placedOrders, setPlacedOrders] = useState<OrderResponse[] | null>(null);
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [shippingName, setShippingName] = useState("");
  const [shippingPhone, setShippingPhone] = useState("");

  useEffect(() => {
    if (!user || user.role !== "CUSTOMER") return;

    let active = true;
    setIsLoading(true);

    getCart()
      .then((res) => {
        if (active) setCart(res.data);
      })
      .catch(() => {
        if (active) toast.error("Không thể tải giỏ hàng.");
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [user]);

  if (!user) {
    return <Navigate replace state={{ from: "/cart" }} to="/auth" />;
  }

  if (user.role !== "CUSTOMER") {
    return <Navigate replace to="/" />;
  }

  const handleQuantityChange = async (itemId: string, newQty: number) => {
    if (newQty < 1) return;
    setUpdatingItemId(itemId);
    try {
      const res = await updateCartItem(itemId, { quantity: newQty });
      setCart(res.data);
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setUpdatingItemId(null);
    }
  };

  const handleRemove = async (itemId: string) => {
    setUpdatingItemId(itemId);
    try {
      await removeCartItem(itemId);
      setCart((prev) =>
        prev
          ? {
              ...prev,
              items: prev.items.filter((i) => i.id !== itemId),
            }
          : prev,
      );
      toast.info("Đã xoá vật phẩm khỏi giỏ.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setUpdatingItemId(null);
    }
  };

  const openAddressForm = async () => {
    if (!shippingName && !shippingPhone) {
      try {
        const res = await getCustomerProfile();
        if (res.data) {
          setShippingName(res.data.fullName ?? user?.fullName ?? "");
          setShippingPhone(res.data.phone ?? "");
        }
      } catch {
        setShippingName(user?.fullName ?? "");
      }
    }
    setShowAddressForm(true);
  };

  const handleCheckout = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!shippingName.trim() || !shippingPhone.trim()) {
      toast.error("Vui lòng điền đầy đủ Họ tên và Số điện thoại.");
      return;
    }
    setIsCheckingOut(true);
    setShowAddressForm(false);
    try {
      const res = await checkoutCart({
        note: checkoutNote.trim() || undefined,
        shippingName: shippingName.trim(),
        shippingPhone: shippingPhone.trim(),
      });
      setPlacedOrders(res.data);
      toast.success("Đặt hàng thành công!");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Thanh toán thất bại. Vui lòng thử lại."));
    } finally {
      setIsCheckingOut(false);
    }
  };

  const items = cart?.items ?? [];
  const totalAmount = cart?.totalAmount ?? 0;

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <div className="relative z-10 mx-auto max-w-4xl">
        <div className="mb-8 flex items-center gap-4">
          <Link
            className="inline-flex items-center rounded-full border border-white/70 bg-white/65 px-5 py-2.5 text-sm font-bold text-black-blue shadow-glass-inset transition hover:-translate-y-0.5 hover:border-aqua hover:bg-white"
            to="/merch"
          >
            ← Tiếp tục mua sắm
          </Link>
          <h1 className="font-fredoka text-3xl font-bold text-black-blue">
            Giỏ hàng
          </h1>
        </div>

        {isLoading ? (
          <div className="py-20 text-center text-sm text-ink/60">
            Đang tải giỏ hàng...
          </div>
        ) : placedOrders ? (
          <OrderSuccess
            orders={placedOrders}
            onContinue={() => navigate("/merch")}
          />
        ) : items.length === 0 ? (
          <EmptyCart />
        ) : (
          <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
            <section className="rounded-panel border border-white/50 bg-white/35 shadow-glass backdrop-blur-xl">
              <div className="divide-y divide-white/40">
                {items.map((item) => {
                  const isUpdating = updatingItemId === item.id;
                  const merch = item.merch;
                  const img =
                    merch.images?.[0] ||
                    "https://placehold.co/200x200/e9feff/1a3a4a?font=montserrat&text=MERCH";

                  return (
                    <div className="flex gap-4 p-4 sm:p-5" key={item.id}>
                      <Link to={`/merch/${merch.id}`}>
                        <img
                          alt={merch.name}
                          className="h-20 w-20 flex-none rounded-2xl object-cover mix-blend-multiply"
                          src={img}
                        />
                      </Link>

                      <div className="min-w-0 flex-1">
                        <Link
                          className="font-fredoka text-lg font-bold leading-tight text-black-blue hover:text-ink"
                          to={`/merch/${merch.id}`}
                        >
                          {merch.name}
                        </Link>
                        <p className="mt-0.5 text-xs text-ink/55">
                          {formatPrice(merch.price)} / sản phẩm
                        </p>

                        <div className="mt-3 flex items-center gap-3">
                          <div className="flex h-9 items-center rounded-full bg-white/55 px-3 shadow-glass-inset">
                            <button
                              aria-label="Giảm"
                              className="px-1 text-lg text-ink/55 hover:text-black-blue disabled:opacity-30"
                              disabled={isUpdating || item.quantity <= 1}
                              onClick={() =>
                                handleQuantityChange(item.id, item.quantity - 1)
                              }
                              type="button"
                            >
                              -
                            </button>
                            <span className="w-8 text-center text-sm font-semibold text-black-blue">
                              {item.quantity}
                            </span>
                            <button
                              aria-label="Tăng"
                              className="px-1 text-lg text-black-blue hover:scale-110 disabled:opacity-30"
                              disabled={isUpdating || item.quantity >= merch.stock}
                              onClick={() =>
                                handleQuantityChange(item.id, item.quantity + 1)
                              }
                              type="button"
                            >
                              +
                            </button>
                          </div>

                          <button
                            className="text-xs text-ink/40 hover:text-peach disabled:opacity-30"
                            disabled={isUpdating}
                            onClick={() => handleRemove(item.id)}
                            type="button"
                          >
                            Xoá
                          </button>
                        </div>
                      </div>

                      <div className="flex flex-col items-end justify-between py-1">
                        <p className="font-fredoka font-bold text-black-blue">
                          {formatPrice(item.subtotal)}
                        </p>
                        {isUpdating && (
                          <span className="text-xs text-ink/40 animate-pulse">
                            Đang cập nhật...
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>

            <aside className="self-start rounded-panel border border-white/50 bg-white/45 shadow-glass backdrop-blur-xl overflow-hidden">
              <div className="p-6">
                <h2 className="font-fredoka text-2xl font-bold text-black-blue">
                  Thanh toán
                </h2>

                <div className="mt-4 space-y-2 border-b border-ink/10 pb-4 text-sm">
                  <div className="flex justify-between text-ink/65">
                    <span>Tổng sản phẩm</span>
                    <span>{items.reduce((s, i) => s + i.quantity, 0)} sản phẩm</span>
                  </div>
                  <div className="flex justify-between font-bold text-black-blue">
                    <span>Tổng tiền</span>
                    <span>{formatPrice(totalAmount)}</span>
                  </div>
                </div>

                {!showAddressForm ? (
                  <>
                    <div className="mt-4">
                      <label className="block text-xs font-semibold text-ink/70 mb-1">
                        Ghi chú (tùy chọn)
                      </label>
                      <textarea
                        className="w-full resize-none rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua h-16"
                        onChange={(e) => setCheckoutNote(e.target.value)}
                        placeholder="Nhắn gì đó với ban tổ chức..."
                        value={checkoutNote}
                      />
                    </div>

                    <button
                      className="mt-4 w-full rounded-full bg-black-blue py-3 text-sm font-bold text-white transition hover:-translate-y-0.5 hover:bg-ink disabled:cursor-not-allowed disabled:opacity-50"
                      disabled={isCheckingOut || items.length === 0}
                      onClick={openAddressForm}
                      type="button"
                    >
                      {isCheckingOut ? "Đang xử lý..." : "Đặt trước"}
                    </button>

                    <p className="mt-3 text-center text-xs text-ink/45">
                      Nhận hàng tại trường
                    </p>
                  </>
                ) : (
                  <form className="mt-4 space-y-3" onSubmit={handleCheckout}>
                    <div className="flex items-center justify-between border-b border-ink/10 pb-3 mb-1">
                      <p className="font-fredoka font-bold text-black-blue text-base">
                        Thông tin đặt trước
                      </p>
                      <button
                        className="text-xs text-ink/50 hover:text-black-blue"
                        onClick={() => setShowAddressForm(false)}
                        type="button"
                      >
                        Huỷ
                      </button>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-ink/70 mb-1">Họ và tên *</label>
                      <input
                        autoFocus
                        className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                        onChange={e => setShippingName(e.target.value)}
                        placeholder="Tên người nhận"
                        required
                        value={shippingName}
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-ink/70 mb-1">Số điện thoại *</label>
                      <input
                        className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                        onChange={e => setShippingPhone(e.target.value)}
                        placeholder="Số điện thoại liên lạc"
                        required
                        type="tel"
                        value={shippingPhone}
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-ink/70 mb-1">Ghi chú (tùy chọn)</label>
                      <input
                        className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                        onChange={e => setCheckoutNote(e.target.value)}
                        placeholder="Lời nhắn cho ban tổ chức..."
                        value={checkoutNote}
                      />
                    </div>

                    <button
                      className="w-full rounded-full bg-aqua py-3 text-sm font-bold text-black-blue transition hover:bg-white hover:shadow-glass disabled:opacity-50"
                      disabled={isCheckingOut}
                      type="submit"
                    >
                      {isCheckingOut ? "Đang xử lý..." : "Xác nhận đặt trước"}
                    </button>
                  </form>
                )}
              </div>
            </aside>
          </div>
        )}
      </div>
    </main>
  );
}
