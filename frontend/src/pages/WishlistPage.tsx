import { lazy, Suspense, useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { getApiErrorMessage } from "../api/auth";
import { addCartItem } from "../api/cart";
import { getWishlist, removeFromWishlist } from "../api/wishlist";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { WishlistItemResponse } from "../types/shared";

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

function WishlistCard({
  item,
  onRemove,
  onAddToCart,
  isRemoving,
  isAddingToCart,
}: {
  item: WishlistItemResponse;
  onRemove: (merchId: string) => void;
  onAddToCart: (merchId: string) => void;
  isRemoving: boolean;
  isAddingToCart: boolean;
}) {
  const merch = item.merch;
  const img =
    merch.images?.[0] ||
    "https://placehold.co/400x400/e9feff/1a3a4a?font=montserrat&text=MERCH";

  return (
    <div className="flex flex-col overflow-hidden rounded-[28px] border border-white/50 bg-white/40 shadow-glass backdrop-blur-xl transition hover:-translate-y-1">
      <Link to={`/merch/${merch.id}`}>
        <div className="aspect-square overflow-hidden bg-white/30">
          <img
            alt={merch.name}
            className="size-full object-cover mix-blend-multiply"
            src={img}
          />
        </div>
      </Link>

      <div className="flex flex-1 flex-col gap-3 p-4">
        <div>
          <Link
            className="font-fredoka text-lg font-bold leading-tight text-black-blue hover:text-ink"
            to={`/merch/${merch.id}`}
          >
            {merch.name}
          </Link>
          <p className="mt-1 text-sm font-semibold text-ink/65">
            {formatPrice(merch.price)}
          </p>
          {merch.stock <= 0 && (
            <span className="mt-1 inline-block rounded-full bg-peach/30 px-2 py-0.5 text-[10px] font-semibold text-black-blue">
              Hết hàng
            </span>
          )}
        </div>

        <div className="mt-auto flex flex-col gap-2">
          <button
            className="w-full rounded-full bg-black-blue py-2 text-xs font-bold text-white transition hover:-translate-y-0.5 hover:bg-ink disabled:cursor-not-allowed disabled:opacity-50"
            disabled={isAddingToCart || merch.stock <= 0}
            onClick={() => onAddToCart(merch.id)}
            type="button"
          >
            {isAddingToCart ? "Đang thêm..." : "Thêm vào giỏ"}
          </button>
          <button
            className="w-full rounded-full border border-peach/50 bg-peach/10 py-2 text-xs font-semibold text-black-blue transition hover:bg-peach/30 disabled:opacity-50"
            disabled={isRemoving}
            onClick={() => onRemove(merch.id)}
            type="button"
          >
            {isRemoving ? "Đang xoá..." : "Xoá khỏi yêu thích"}
          </button>
        </div>
      </div>
    </div>
  );
}

export function WishlistPage() {
  const user = useAuthStore((s) => s.user);
  const [items, setItems] = useState<WishlistItemResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [addingToCartId, setAddingToCartId] = useState<string | null>(null);

  useEffect(() => {
    if (!user || user.role !== "CUSTOMER") return;

    let active = true;
    setIsLoading(true);

    getWishlist()
      .then((res) => {
        if (active) setItems(res.data?.items ?? []);
      })
      .catch(() => {
        if (active) toast.error("Không thể tải danh sách yêu thích.");
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [user]);

  if (!user) {
    return <Navigate replace state={{ from: "/wishlist" }} to="/auth" />;
  }

  if (user.role !== "CUSTOMER") {
    return <Navigate replace to="/" />;
  }

  const handleRemove = async (merchId: string) => {
    setRemovingId(merchId);
    try {
      await removeFromWishlist(merchId);
      setItems((prev) => prev.filter((i) => i.merch.id !== merchId));
      toast.info("Đã xoá khỏi danh sách yêu thích.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setRemovingId(null);
    }
  };

  const handleAddToCart = async (merchId: string) => {
    setAddingToCartId(merchId);
    try {
      await addCartItem({ merchId, quantity: 1 });
      toast.success("Đã thêm vào giỏ hàng!");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Không thể thêm vào giỏ hàng."));
    } finally {
      setAddingToCartId(null);
    }
  };

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <div className="relative z-10 mx-auto max-w-6xl">
        <div className="mb-8">
          <h1 className="font-fredoka text-4xl font-bold text-black-blue">
            Vật phẩm yêu thích
          </h1>
          <p className="mt-2 text-sm text-ink/60">
            {items.length} vật phẩm đã lưu
          </p>
        </div>

        {isLoading ? (
          <div className="py-20 text-center text-sm text-ink/60">
            Đang tải danh sách yêu thích...
          </div>
        ) : items.length === 0 ? (
          <div className="flex min-h-[40vh] flex-col items-center justify-center rounded-panel border border-white/50 bg-white/35 px-8 py-16 text-center shadow-glass backdrop-blur-xl">
            <p className="font-fredoka text-5xl">♡</p>
            <h2 className="mt-4 font-fredoka text-2xl font-bold text-black-blue">
              Chưa có vật phẩm yêu thích
            </h2>
            <p className="mt-2 text-sm text-ink/65">
              Bấm vào nút yêu thích trên các sản phẩm để lưu lại đây.
            </p>
            <Link
              className="mt-6 rounded-full bg-black-blue px-8 py-3 text-sm font-bold text-white transition hover:-translate-y-0.5 hover:bg-ink"
              to="/merch"
            >
              Khám phá vật phẩm
            </Link>
          </div>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
            {items.map((item) => (
              <WishlistCard
                isAddingToCart={addingToCartId === item.merch.id}
                isRemoving={removingId === item.merch.id}
                item={item}
                key={item.id}
                onAddToCart={handleAddToCart}
                onRemove={handleRemove}
              />
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
