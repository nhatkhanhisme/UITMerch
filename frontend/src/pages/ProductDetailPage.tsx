import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { findProductById, MOCK_PRODUCTS } from "../mocks/merchData";
import type { MockProduct } from "../mocks/merchData";
import { getPublicMerchDetail } from "../api/merch";
import { getPublicOrganizationDetail } from "../api/organization";
import { createGuestCheckoutOrder } from "../api/order";
import { mapMerchToMockProduct } from "../types/shared";
import { toast } from "../stores/toastStore";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency",
});

function formatPrice(price?: number) {
  return price !== undefined && price > 0
    ? currencyFormatter.format(price)
    : "Sự kiện / Miễn phí";
}

function ProductNotFound() {
  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <section className="relative z-10 mx-auto flex min-h-[60vh] max-w-3xl flex-col items-center justify-center rounded-panel border border-white/50 bg-white/35 px-8 text-center shadow-glass backdrop-blur-xl">
        <p className="font-sans text-sm font-semibold uppercase text-ink/50">
          Không tìm thấy
        </p>
        <h1 className="mt-3 font-fredoka text-4xl font-bold text-black-blue">
          Vật phẩm này chưa có trong kho
        </h1>
        <p className="mt-4 max-w-xl text-sm leading-7 text-ink/65">
          Có thể đường dẫn đã thay đổi hoặc sản phẩm chưa được mở bán. Quay lại
          kho vật phẩm để xem các món đang có.
        </p>
        <Link
          className="mt-8 rounded-full bg-black-blue px-8 py-3 text-sm font-bold text-white transition hover:-translate-y-0.5 hover:bg-ink"
          to="/merch"
        >
          Quay về kho vật phẩm
        </Link>
      </section>
    </main>
  );
}

function Gallery({
  image,
  name,
}: {
  image: string;
  name: string;
}) {
  return (
    <div className="relative overflow-hidden rounded-[32px] bg-white/25 p-4 shadow-glass-inset">
      <div className="aspect-square overflow-hidden rounded-[32px] bg-white/40">
        <img
          alt={name}
          className="size-full object-cover mix-blend-multiply"
          src={image}
        />
      </div>
    </div>
  );
}

function QuantityStepper({
  quantity,
  stock,
  onChange,
}: {
  quantity: number;
  stock: number;
  onChange: (nextQuantity: number) => void;
}) {
  return (
    <div className="flex h-14 w-full max-w-[236px] items-center justify-between rounded-full bg-white/55 px-5 shadow-glass-inset">
      <button
        aria-label="Giảm số lượng"
        className="text-2xl text-ink/55 transition hover:text-black-blue disabled:opacity-30"
        disabled={quantity <= 1}
        onClick={() => onChange(Math.max(1, quantity - 1))}
        type="button"
      >
        -
      </button>
      <span className="text-lg font-semibold text-black-blue">{quantity}</span>
      <button
        aria-label="Tăng số lượng"
        className="text-2xl text-black-blue transition hover:scale-110 disabled:opacity-30"
        disabled={quantity >= stock}
        onClick={() => onChange(Math.min(stock, quantity + 1))}
        type="button"
      >
        +
      </button>
    </div>
  );
}

function PurchasePanel({
  product,
}: {
  product: MockProduct;
}) {
  const [quantity, setQuantity] = useState(1);

  // Guest Checkout Flow States
  const [showCheckout, setShowCheckout] = useState(false);
  const [guestName, setGuestName] = useState("");
  const [guestPhone, setGuestPhone] = useState("");
  const [guestAddress, setGuestAddress] = useState("");
  const [guestEmail, setGuestEmail] = useState("");
  const [note, setNote] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [orderPlaced, setOrderPlaced] = useState(false);

  const handleCheckoutSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!guestName.trim() || !guestPhone.trim() || !guestAddress.trim()) {
      toast.error("Vui lòng điền đầy đủ Tên, Số điện thoại và Địa chỉ giao hàng.");
      return;
    }

    setIsSubmitting(true);
    try {
      await createGuestCheckoutOrder({
        guestAddress,
        guestEmail: guestEmail.trim() || undefined,
        guestName,
        guestPhone,
        items: [
          {
            merchId: product.id,
            quantity,
          },
        ],
        note: note.trim() || undefined,
      });

      setOrderPlaced(true);
      toast.success("Đặt hàng thành công! Đơn hàng sẽ được thanh toán COD.");
    } catch {
      toast.error(
        "Có lỗi xảy ra khi tạo đơn hàng. Vui lòng thử lại sau giây lát.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  if (orderPlaced) {
    return (
      <aside className="self-start rounded-panel border border-aqua/60 bg-white/70 p-6 shadow-glass backdrop-blur-xl lg:fixed lg:right-16 lg:top-28 lg:w-[430px] xl:right-[max(4rem,calc((100vw-1320px)/2))] xl:w-[480px]">
        <div className="text-center py-6">
          <div className="mx-auto flex size-16 items-center justify-center rounded-full bg-aqua/20 text-3xl text-black-blue mb-4">
            ✓
          </div>
          <h2 className="font-fredoka text-3xl font-bold text-black-blue">
            Đặt hàng thành công!
          </h2>
          <p className="mt-3 text-sm leading-6 text-ink/70">
            Cảm ơn <strong>{guestName}</strong> đã ủng hộ sản phẩm của{" "}
            <strong>{product.orgName}</strong>.
          </p>

          <div className="mt-6 rounded-2xl bg-white/50 p-4 text-left border border-white/60 space-y-2 text-xs">
            <p className="font-semibold text-black-blue border-b pb-2 text-sm">
              Thông tin đơn hàng (COD)
            </p>
            <p>
              <span className="text-ink/60">Vật phẩm:</span> {product.name}
            </p>
            <p>
              <span className="text-ink/60">Phân loại:</span> {product.category}
            </p>
            <p>
              <span className="text-ink/60">Số lượng:</span> {quantity}
            </p>
            <p>
              <span className="text-ink/60">SĐT:</span> {guestPhone}
            </p>
            <p>
              <span className="text-ink/60">Giao đến:</span> {guestAddress}
            </p>
            <p className="pt-2 font-bold text-black-blue border-t mt-2 flex justify-between text-sm">
              <span>Tổng thanh toán:</span>
              <span>{formatPrice((product.price ?? 0) * quantity)}</span>
            </p>
          </div>

          <button
            className="mt-6 w-full rounded-full bg-black-blue py-3 text-sm font-bold text-white transition hover:bg-ink"
            onClick={() => {
              setOrderPlaced(false);
              setShowCheckout(false);
            }}
            type="button"
          >
            Tiếp tục mua sắm
          </button>
        </div>
      </aside>
    );
  }

  return (
    <aside className="self-start rounded-panel border border-white/55 bg-white/45 p-6 shadow-glass backdrop-blur-xl lg:fixed lg:right-16 lg:top-28 lg:w-[430px] xl:right-[max(4rem,calc((100vw-1320px)/2))] xl:w-[480px]">
      <div className="border-b border-ink/10 pb-6">
        <Link
          className="inline-flex items-center rounded-full border border-white/70 bg-white/65 px-5 py-2.5 text-sm font-bold text-black-blue shadow-glass-inset transition hover:-translate-y-0.5 hover:border-aqua hover:bg-white"
          to="/merch"
        >
          ← Kho vật phẩm
        </Link>
        <p className="mt-6 text-sm font-semibold text-ink/55">
          {product.orgName}
        </p>
        <h1 className="mt-2 font-fredoka text-4xl font-bold leading-tight text-black-blue sm:text-5xl lg:text-4xl xl:text-5xl">
          {product.name}
        </h1>
        <p className="mt-4 text-sm leading-7 text-ink/65">{product.description}</p>
      </div>

      <div className="space-y-7 py-7">
        <div>
          <p className="font-fredoka text-4xl font-bold text-black-blue">
            {formatPrice(product.price)}
          </p>
          <p className="mt-2 text-sm text-ink/55">
            {product.stock > 0 ? `Còn ${product.stock} sản phẩm` : "Tạm hết hàng"}
          </p>
        </div>

        {!showCheckout ? (
          <div className="flex flex-col gap-4 sm:flex-row lg:flex-col xl:flex-row">
            <QuantityStepper
              onChange={(nextQuantity) => {
                setQuantity(nextQuantity);
              }}
              quantity={quantity}
              stock={Math.max(product.stock, 1)}
            />
            <button
              className="min-h-14 flex-1 rounded-full bg-black px-8 text-sm font-bold uppercase text-white transition hover:-translate-y-0.5 hover:bg-black-blue disabled:cursor-not-allowed disabled:opacity-45"
              disabled={product.stock <= 0}
              onClick={() => setShowCheckout(true)}
              type="button"
            >
              Mua ngay COD
            </button>
          </div>
        ) : (
          <form
            className="rounded-[28px] border border-white/80 bg-white/60 p-4 space-y-4 shadow-glass-inset animate-fadeIn"
            onSubmit={handleCheckoutSubmit}
          >
            <div className="flex items-center justify-between border-b pb-2">
              <p className="font-fredoka font-bold text-black-blue">
                Thông tin nhận hàng (COD)
              </p>
              <button
                className="text-xs text-ink/50 hover:text-black-blue"
                onClick={() => setShowCheckout(false)}
                type="button"
              >
                Hủy
              </button>
            </div>

            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">
                Họ và tên *
              </label>
              <input
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={(e) => setGuestName(e.target.value)}
                placeholder="Nhập tên người nhận"
                required
                value={guestName}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">
                Số điện thoại *
              </label>
              <input
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={(e) => setGuestPhone(e.target.value)}
                placeholder="Nhập số điện thoại liên lạc"
                required
                type="tel"
                value={guestPhone}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">
                Địa chỉ giao hàng *
              </label>
              <textarea
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua h-16 resize-none"
                onChange={(e) => setGuestAddress(e.target.value)}
                placeholder="Số nhà, đường, phường/xã, quận/huyện..."
                required
                value={guestAddress}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">
                Email (Tùy chọn)
              </label>
              <input
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={(e) => setGuestEmail(e.target.value)}
                placeholder="Để nhận thông báo cập nhật đơn"
                type="email"
                value={guestEmail}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">
                Ghi chú thêm
              </label>
              <input
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={(e) => setNote(e.target.value)}
                placeholder="Lời nhắn cho ban tổ chức..."
                value={note}
              />
            </div>

            <div className="pt-2">
              <button
                className="w-full rounded-full bg-aqua py-3 text-sm font-bold text-black-blue transition hover:bg-white hover:shadow-glass"
                disabled={isSubmitting}
                type="submit"
              >
                {isSubmitting ? "Đang xử lý..." : "Xác nhận đặt hàng COD"}
              </button>
            </div>
          </form>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3 border-t border-ink/10 pt-6">
        <button
          className="rounded-full border border-ink/15 bg-white/35 px-4 py-3 text-sm font-semibold text-black-blue transition hover:border-aqua hover:bg-white/55"
          type="button"
        >
          ♡ Yêu thích
        </button>
        <button
          className="rounded-full border border-ink/15 bg-white/35 px-4 py-3 text-sm font-semibold text-black-blue transition hover:border-aqua hover:bg-white/55"
          type="button"
        >
          Chia sẻ
        </button>
      </div>
    </aside>
  );
}

export function ProductDetailPage() {
  const { id } = useParams();
  const fallbackProduct = findProductById(id);

  const [liveProduct, setLiveProduct] = useState<MockProduct | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (!id) {
      return;
    }

    let isActive = true;
    setIsLoading(true);

    async function fetchProduct() {
      try {
        const res = await getPublicMerchDetail(id as string);
        if (isActive && res?.data) {
          let orgMap: Record<string, string> | undefined;
          try {
            const orgRes = await getPublicOrganizationDetail(res.data.orgId);
            if (orgRes?.data) {
              orgMap = { [res.data.orgId]: orgRes.data.name };
            }
          } catch {
            // keep standard mapping
          }

          const mapped = mapMerchToMockProduct(res.data, orgMap);
          setLiveProduct(mapped);
        }
      } catch {
        // Fall back gracefully to local static mock product if missing
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    void fetchProduct();

    return () => {
      isActive = false;
    };
  }, [id]);

  const product = liveProduct || fallbackProduct;

  const relatedProducts = useMemo(
    () =>
      product
        ? MOCK_PRODUCTS.filter(
            (item) => item.id !== product.id && item.category === product.category,
          ).slice(0, 3)
        : [],
    [product],
  );

  if (!product && !isLoading) {
    return <ProductNotFound />;
  }

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <div className="relative z-10 mx-auto max-w-[1320px]">
        {product ? (
          <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_430px] xl:grid-cols-[minmax(0,1fr)_480px]">
            <section className="overflow-hidden rounded-panel border border-white/50 bg-white/30 p-4 shadow-glass backdrop-blur-xl sm:p-5">
              <Gallery
                image={product.image}
                name={product.name}
              />

              <section className="mt-6 border-t border-white/50 px-2 py-7 sm:px-3">
                <p className="text-sm font-bold uppercase text-ink/45">
                  {product.category}
                </p>
                <h2 className="mt-2 font-fredoka text-3xl font-bold text-black-blue">
                  {product.name}
                </h2>
                <p className="mt-3 text-base font-semibold text-ink/70">
                  {product.orgName}
                </p>
                <div className="mt-6 grid gap-4 sm:grid-cols-2">
                  <div className="rounded-[28px] border border-white/50 bg-white/35 p-5">
                    <p className="text-xs uppercase text-ink/45">Tồn kho</p>
                    <p className="mt-2 font-semibold text-black-blue">
                      {product.stock} sản phẩm
                    </p>
                  </div>
                  <div className="rounded-[28px] border border-white/50 bg-white/35 p-5">
                    <p className="text-xs uppercase text-ink/45">Danh mục</p>
                    <p className="mt-2 font-semibold text-black-blue">
                      {product.category}
                    </p>
                  </div>
                </div>
              </section>

              {relatedProducts.length > 0 ? (
                <section className="border-t border-white/50 px-2 py-7 sm:px-3">
                  <h2 className="font-fredoka text-3xl font-bold text-black-blue">
                    Vật phẩm liên quan
                  </h2>
                  <div className="mt-5 grid gap-4 sm:grid-cols-3">
                    {relatedProducts.map((item) => (
                      <Link
                        className="rounded-[28px] border border-white/50 bg-white/35 p-3 transition hover:-translate-y-1 hover:border-aqua"
                        key={item.id}
                        to={`/merch/${item.id}`}
                      >
                        <img
                          alt={item.name}
                          className="aspect-square w-full rounded-[22px] object-cover mix-blend-multiply"
                          src={item.image}
                        />
                        <p className="mt-3 font-fredoka text-lg font-bold text-black-blue">
                          {item.name}
                        </p>
                        <p className="mt-1 text-sm font-semibold text-ink/70">
                          {formatPrice(item.price)}
                        </p>
                      </Link>
                    ))}
                  </div>
                </section>
              ) : null}
            </section>

            <PurchasePanel
              key={product.id}
              product={product}
            />
          </div>
        ) : (
          <div className="py-24 text-center">Đang tải thông tin vật phẩm...</div>
        )}
      </div>
    </main>
  );
}
