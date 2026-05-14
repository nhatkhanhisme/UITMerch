import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { FeaturedSlider } from "../components/features/FeaturedSlider";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { ProductGrid } from "../components/features/ProductGrid";
import type { MockProduct } from "../mocks/merchData";
import { getCatalogProducts, getPopularProducts } from "../api/catalog";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  }))
);

const PAGE_SIZE = 8;

const FILTER_OPTIONS: FilterOption[] = [
  { label: "A → Z", value: "az" },
  { label: "Z → A", value: "za" },
  { label: "Giá tăng dần", value: "price-asc" },
  { label: "Giá giảm dần", value: "price-desc" },
];

export function MerchPage() {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [products, setProducts] = useState<MockProduct[]>([]);
  const [featuredProducts, setFeaturedProducts] = useState<MockProduct[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    const loadProducts = async () => {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const [productList, popularList] = await Promise.all([
          getCatalogProducts(),
          getPopularProducts(),
        ]);

        if (!isActive) {
          return;
        }

        setProducts(productList.items);
        setFeaturedProducts(popularList);
      } catch {
        if (!isActive) {
          return;
        }

        setProducts([]);
        setFeaturedProducts([]);
        setErrorMessage(
          "Chưa tải được danh sách vật phẩm từ backend. Bạn có thể bật VITE_USE_MOCK=true để dùng dữ liệu demo.",
        );
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    };

    void loadProducts();

    return () => {
      isActive = false;
    };
  }, []);

  const processed = useMemo(() => {
    let list = products.filter(
      (p) =>
        p.name.toLowerCase().includes(query.toLowerCase()) ||
        p.orgName.toLowerCase().includes(query.toLowerCase()) ||
        p.category.toLowerCase().includes(query.toLowerCase())
    );

    switch (activeFilter) {
      case "az":
        list = [...list].sort((a, b) => a.name.localeCompare(b.name, "vi"));
        break;
      case "za":
        list = [...list].sort((a, b) => b.name.localeCompare(a.name, "vi"));
        break;
      case "price-asc":
        list = [...list].sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
        break;
      case "price-desc":
        list = [...list].sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
        break;
    }

    return list;
  }, [query, activeFilter, products]);

  const totalPages = Math.max(1, Math.ceil(processed.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const paginated = processed.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

  const handleQueryChange = (value: string) => {
    setQuery(value);
    setPage(1);
  };

  const handleFilterChange = (value: string | null) => {
    setActiveFilter(value);
    setPage(1);
  };

  return (
    <main className="relative min-h-screen bg-transparent pb-10 pt-28 px-5 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <div className="z-0">
          <ShaderBackground />
        </div>
      </Suspense>

      <div className="relative z-10">
        <GlassContainer>
          <header className="mb-8">
            <h1 className="font-fredoka text-3xl font-bold text-black-blue sm:text-4xl">
              Kho Vật Phẩm
            </h1>
            <p className="mt-2 font-sans text-sm text-ink/60">
              {processed.length} vật phẩm
              {query ? ` phù hợp với "${query}"` : ""}
            </p>
          </header>

          {isLoading ? (
            <div className="mb-8 rounded-[28px] border border-white/50 bg-white/40 p-6 text-center font-sans text-sm text-ink/60 shadow-glass">
              Đang tải vật phẩm từ backend...
            </div>
          ) : null}

          {!isLoading && errorMessage ? (
            <div className="mb-8 rounded-[28px] border border-peach bg-peach/15 p-6 font-sans text-sm leading-6 text-black-blue">
              {errorMessage}
            </div>
          ) : null}

          <FeaturedSlider items={featuredProducts} />

          <MerchToolbar
            activeFilter={activeFilter}
            filterOptions={FILTER_OPTIONS}
            onFilterChange={handleFilterChange}
            onQueryChange={handleQueryChange}
            query={query}
          />

          <ProductGrid
            emptyMessage={
              errorMessage
                ? "Chưa có vật phẩm để hiển thị."
                : "Không tìm thấy vật phẩm nào."
            }
            products={paginated}
          />

          <Pagination
            currentPage={currentPage}
            onPageChange={setPage}
            totalPages={totalPages}
          />
        </GlassContainer>
      </div>
    </main>
  );
}
