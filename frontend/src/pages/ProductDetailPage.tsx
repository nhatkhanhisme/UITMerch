import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import type { MockProduct, ProductColorOption } from "../mocks/merchData";
import { getCatalogProductById, getCatalogProducts } from "../api/catalog";

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
  activeImage,
  images,
  name,
  onSelect,
}: {
  activeImage: string;
  images: string[];
  name: string;
  onSelect: (image: string) => void;
}) {
  const activeIndex = Math.max(0, images.indexOf(activeImage));

  const goTo = (direction: -1 | 1) => {
    const nextIndex = (activeIndex + direction + images.length) % images.length;
    onSelect(images[nextIndex]);
  };

  return (
    <section className="space-y-5">
      <div className="relative overflow-hidden rounded-[32px] bg-white/25 p-4 shadow-glass-inset">
        <div className="aspect-square overflow-hidden rounded-[32px] bg-white/40">
          <img
            alt={name}
            className="size-full object-cover mix-blend-multiply"
            src={activeImage}
          />
        </div>

        {images.length > 1 ? (
          <>
            <button
              aria-label="Ảnh trước"
              className="absolute left-6 top-1/2 flex size-11 -translate-y-1/2 items-center justify-center rounded-full border border-white/60 bg-white/70 text-2xl text-black-blue shadow-glass backdrop-blur transition hover:-translate-x-0.5 hover:bg-white"
              onClick={() => goTo(-1)}
              type="button"
            >
              ‹
            </button>
            <button
              aria-label="Ảnh kế tiếp"
              className="absolute right-6 top-1/2 flex size-11 -translate-y-1/2 items-center justify-center rounded-full border border-white/60 bg-white/70 text-2xl text-black-blue shadow-glass backdrop-blur transition hover:translate-x-0.5 hover:bg-white"
              onClick={() => goTo(1)}
              type="button"
            >
              ›
            </button>
          </>
        ) : null}
      </div>

      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
        {images.map((image, index) => (
          <button
            aria-label={`Xem ảnh ${index + 1}`}
            className={[
              "aspect-square overflow-hidden rounded-[24px] border bg-white/35 p-1 transition",
              image === activeImage
                ? "border-black-blue shadow-glass"
                : "border-white/50 hover:border-aqua",
            ].join(" ")}
            key={image}
            onClick={() => onSelect(image)}
            type="button"
          >
            <img
              alt=""
              className="size-full rounded-[20px] object-cover mix-blend-multiply"
              src={image}
            />
          </button>
        ))}
      </div>
    </section>
  );
}

function ColorSelector({
  colors,
  selectedColor,
  onSelect,
}: {
  colors: ProductColorOption[];
  selectedColor: ProductColorOption;
  onSelect: (color: ProductColorOption) => void;
}) {
  return (
    <div>
      <p className="text-sm font-medium text-ink/60">
        Màu sắc: <span className="text-ink">{selectedColor.name}</span>
      </p>
      <div className="mt-3 flex flex-wrap gap-3">
        {colors.map((color) => (
          <button
            aria-label={`Chọn màu ${color.name}`}
            className={[
              "flex size-12 items-center justify-center rounded-full border bg-white/60 p-1 transition",
              selectedColor.name === color.name
                ? "border-black-blue ring-2 ring-black-blue/15"
                : "border-white/70 hover:border-aqua",
            ].join(" ")}
            key={color.name}
            onClick={() => onSelect(color)}
            type="button"
          >
            <span
              className="size-full rounded-full border border-black/10"
              style={{ background: color.value }}
            />
          </button>
        ))}
      </div>
    </div>
  );
}

function SizeSelector({
  label,
  options,
  selectedSize,
  onSelect,
}: {
  label: string;
  options: string[];
  selectedSize: string;
  onSelect: (size: string) => void;
}) {
  return (
    <div>
      <p className="text-sm font-medium text-ink/60">
        {label}: <span className="text-ink">{selectedSize}</span>
      </p>
      <div className="mt-3 flex flex-wrap gap-3">
        {options.map((size) => (
          <button
            className={[
              "min-h-12 min-w-16 rounded-none border px-5 text-sm font-bold transition",
              selectedSize === size
                ? "border-black bg-black text-white"
                : "border-ink/20 bg-white/50 text-black-blue hover:border-black-blue",
            ].join(" ")}
            key={size}
            onClick={() => onSelect(size)}
            type="button"
          >
            {size}
          </button>
        ))}
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
  onImageSelect,
  product,
}: {
  onImageSelect: (image: string) => void;
  product: MockProduct;
}) {
  const [selectedColor, setSelectedColor] = useState(product.colors[0]);
  const [selectedSize, setSelectedSize] = useState(product.sizeOptions[0]);
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);

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
        <ColorSelector
          colors={product.colors}
          onSelect={(color) => {
            setSelectedColor(color);
            setAdded(false);
            if (color.image) {
              onImageSelect(color.image);
            }
          }}
          selectedColor={selectedColor}
        />

        <SizeSelector
          label={product.sizeLabel}
          onSelect={(size) => {
            setSelectedSize(size);
            setAdded(false);
          }}
          options={product.sizeOptions}
          selectedSize={selectedSize}
        />

        <div>
          <p className="font-fredoka text-4xl font-bold text-black-blue">
            {formatPrice(product.price)}
          </p>
          <p className="mt-2 text-sm text-ink/55">
            {product.stock > 0 ? `Còn ${product.stock} sản phẩm` : "Tạm hết hàng"}
          </p>
        </div>

        <div className="flex flex-col gap-4 sm:flex-row lg:flex-col xl:flex-row">
          <QuantityStepper
            onChange={(nextQuantity) => {
              setQuantity(nextQuantity);
              setAdded(false);
            }}
            quantity={quantity}
            stock={Math.max(product.stock, 1)}
          />
          <button
            aria-live="polite"
            className="min-h-14 flex-1 rounded-full bg-black px-8 text-sm font-bold uppercase text-white transition hover:-translate-y-0.5 hover:bg-black-blue disabled:cursor-not-allowed disabled:opacity-45"
            disabled={product.stock <= 0}
            onClick={() => setAdded(true)}
            type="button"
          >
            {added ? "Đã thêm vào giỏ demo" : "Thêm vào giỏ demo"}
          </button>
        </div>

        {added ? (
          <div className="rounded-[24px] border border-aqua/60 bg-white/55 p-4 text-sm leading-6 text-ink/70">
            Đã thêm {quantity} sản phẩm vào giỏ demo, màu {selectedColor.name},{" "}
            {product.sizeLabel.toLowerCase()} {selectedSize}. Đây là thao tác
            mô phỏng để quay video; checkout thật sẽ nối backend sau.
          </div>
        ) : null}
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
  const [product, setProduct] = useState<MockProduct | null>(null);
  const [allProducts, setAllProducts] = useState<MockProduct[]>([]);
  const [activeImage, setActiveImage] = useState(product?.image ?? "");
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    const loadProduct = async () => {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const [productDetail, productList] = await Promise.all([
          getCatalogProductById(id),
          getCatalogProducts(),
        ]);

        if (!isActive) {
          return;
        }

        setProduct(productDetail);
        setAllProducts(productList.items);
      } catch {
        if (!isActive) {
          return;
        }

        setProduct(null);
        setAllProducts([]);
        setErrorMessage(
          "Chưa tải được chi tiết vật phẩm từ backend. Bạn có thể bật VITE_USE_MOCK=true để dùng dữ liệu demo.",
        );
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    };

    void loadProduct();

    return () => {
      isActive = false;
    };
  }, [id]);

  useEffect(() => {
    setActiveImage(product?.image ?? "");
  }, [product?.id, product?.image]);

  const relatedProducts = useMemo(
    () =>
      product
        ? allProducts.filter(
            (item) => item.id !== product.id && item.category === product.category,
          ).slice(0, 3)
        : [],
    [allProducts, product],
  );

  if (isLoading) {
    return (
      <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
        <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
          <ShaderBackground />
        </Suspense>
        <section className="relative z-10 mx-auto flex min-h-[60vh] max-w-3xl flex-col items-center justify-center rounded-panel border border-white/50 bg-white/35 px-8 text-center shadow-glass backdrop-blur-xl">
          <p className="font-sans text-sm font-semibold uppercase text-ink/50">
            Đang tải
          </p>
          <h1 className="mt-3 font-fredoka text-4xl font-bold text-black-blue">
            Đang tải chi tiết vật phẩm...
          </h1>
        </section>
      </main>
    );
  }

  if (errorMessage) {
    return (
      <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
        <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
          <ShaderBackground />
        </Suspense>
        <section className="relative z-10 mx-auto flex min-h-[60vh] max-w-3xl flex-col items-center justify-center rounded-panel border border-peach bg-white/45 px-8 text-center shadow-glass backdrop-blur-xl">
          <p className="font-sans text-sm font-semibold uppercase text-ink/50">
            Backend chưa sẵn sàng
          </p>
          <h1 className="mt-3 font-fredoka text-4xl font-bold text-black-blue">
            Chưa tải được vật phẩm
          </h1>
          <p className="mt-4 max-w-xl text-sm leading-7 text-ink/65">
            {errorMessage}
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

  if (!product) {
    return <ProductNotFound />;
  }

  const images = product.gallery.length > 0 ? product.gallery : [product.image];
  const shownImage = activeImage || images[0];

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <div className="relative z-10 mx-auto max-w-[1320px]">
        <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_430px] xl:grid-cols-[minmax(0,1fr)_480px]">
          <section className="overflow-hidden rounded-panel border border-white/50 bg-white/30 p-4 shadow-glass backdrop-blur-xl sm:p-5">
            <Gallery
              activeImage={shownImage}
              images={images}
              name={product.name}
              onSelect={setActiveImage}
            />

            <section className="mt-6 border-t border-white/50 px-2 py-7 sm:px-3">
              <p className="text-sm font-bold uppercase text-ink/45">
                {product.category}
              </p>
              <h2 className="mt-2 font-fredoka text-3xl font-bold text-black-blue">
                Tên tổ chức
              </h2>
              <p className="mt-3 text-base font-semibold text-ink/70">
                {product.orgName}
              </p>
              <div className="mt-6 grid gap-4 sm:grid-cols-3">
                <div className="rounded-[28px] border border-white/50 bg-white/35 p-5">
                  <p className="text-xs uppercase text-ink/45">Chất liệu</p>
                  <p className="mt-2 font-semibold text-black-blue">
                    {product.material ?? "Đang cập nhật"}
                  </p>
                </div>
                <div className="rounded-[28px] border border-white/50 bg-white/35 p-5">
                  <p className="text-xs uppercase text-ink/45">Tồn kho</p>
                  <p className="mt-2 font-semibold text-black-blue">
                    {product.stock} sản phẩm
                  </p>
                </div>
                <div className="rounded-[28px] border border-white/50 bg-white/35 p-5">
                  <p className="text-xs uppercase text-ink/45">Phân loại</p>
                  <p className="mt-2 font-semibold text-black-blue">
                    {product.category}
                  </p>
                </div>
              </div>
            </section>

            {product.detailSections.map((section) => (
              <section
                className="border-t border-white/50 px-2 py-7 sm:px-3"
                key={section.title}
              >
                <h2 className="font-fredoka text-3xl font-bold text-black-blue">
                  {section.title}
                </h2>
                <p className="mt-4 max-w-3xl text-base leading-8 text-ink/70">
                  {section.content}
                </p>
              </section>
            ))}

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
            onImageSelect={setActiveImage}
            product={product}
          />
        </div>
      </div>
    </main>
  );
}
