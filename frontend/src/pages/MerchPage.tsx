import { lazy, Suspense, useEffect, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { FeaturedSlider } from "../components/features/FeaturedSlider";
import type { FeaturedItem } from "../components/features/FeaturedSlider";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { ProductGrid } from "../components/features/ProductGrid";
import type { MockProduct } from "../mocks/merchData";
import { getPublicMerchList, getPopularMerch, getCategories } from "../api/merch";
import { getPublicOrganizations } from "../api/organization";
import { mapMerchToMockProduct } from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const PAGE_SIZE = 8;

// Map UI filter labels → Spring Pageable sort param format
const SORT_MAP: Record<string, string> = {
  az: "name,asc",
  za: "name,desc",
  "price-asc": "price,asc",
  "price-desc": "price,desc",
};

const FILTER_OPTIONS: FilterOption[] = [
  { label: "A → Z", value: "az" },
  { label: "Z → A", value: "za" },
  { label: "Giá tăng dần", value: "price-asc" },
  { label: "Giá giảm dần", value: "price-desc" },
];

export function MerchPage() {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  const [liveProducts, setLiveProducts] = useState<MockProduct[]>([]);
  const [popularProducts, setPopularProducts] = useState<FeaturedItem[] | null>(null);
  const [categoryOptions, setCategoryOptions] = useState<FilterOption[]>([]);
  const [totalItems, setTotalItems] = useState<number | null>(null);
  const [serverTotalPages, setServerTotalPages] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  // One-time: load popular merch + categories
  useEffect(() => {
    let isActive = true;
    async function fetchMeta() {
      try {
        const orgsRes = await getPublicOrganizations({ size: 50 });
        const orgMap: Record<string, string> = {};
        if (orgsRes?.data) {
          orgsRes.data.forEach((o) => { orgMap[o.id] = o.name; });
        }

        const [catRes, popRes] = await Promise.allSettled([
          getCategories(),
          getPopularMerch(),
        ]);

        if (isActive && catRes.status === "fulfilled" && catRes.value?.data) {
          setCategoryOptions(
            catRes.value.data.map((cat) => ({ label: cat.name, value: cat.slug ?? cat.id }))
          );
        }

        if (isActive && popRes.status === "fulfilled" && popRes.value?.data?.length) {
          setPopularProducts(
            popRes.value.data.slice(0, 5).map((item) => ({
              id: item.id,
              name: item.name,
              orgName: orgMap[item.orgId] || "Tổ chức UIT",
              desc: item.description,
              image: item.images?.[0] || null,
              link: `/merch/${item.id}`,
            }))
          );
        } else if (isActive) {
          setPopularProducts([]);
        }
      } catch {
        if (isActive) setPopularProducts([]);
      }
    }
    void fetchMeta();
    return () => { isActive = false; };
  }, []);

  // Paginated merch list — re-fetched on filter/search/page change
  useEffect(() => {
    let isActive = true;
    setIsLoading(true);

    const timer = window.setTimeout(() => {
      async function fetchList() {
        try {
          const res = await getPublicMerchList({
            keyword: query || undefined,
            category: activeCategory || undefined,
            page: page - 1,
            size: PAGE_SIZE,
            sort: activeFilter ? SORT_MAP[activeFilter] : undefined,
          });

          if (isActive && res?.data) {
            const orgsRes = await getPublicOrganizations({ size: 50 });
            const orgMap: Record<string, string> = {};
            if (orgsRes?.data) {
              orgsRes.data.forEach((o) => { orgMap[o.id] = o.name; });
            }

            const mapped = res.data.map((item) => mapMerchToMockProduct(item, orgMap));
            setLiveProducts(mapped);
            setApiError(null);

            if (res.meta) {
              setTotalItems(res.meta.totalElements);
              setServerTotalPages(res.meta.totalPages);
            } else {
              setTotalItems(mapped.length);
              setServerTotalPages(Math.ceil(mapped.length / PAGE_SIZE));
            }
          }
        } catch {
          if (isActive) setApiError("Không thể tải danh sách vật phẩm. Vui lòng thử lại.");
        } finally {
          if (isActive) setIsLoading(false);
        }
      }
      void fetchList();
    }, 300);

    return () => { isActive = false; window.clearTimeout(timer); };
  }, [query, activeFilter, activeCategory, page]);

  const pagesDisplay = serverTotalPages ?? (Math.ceil(liveProducts.length / PAGE_SIZE) || 1);
  const countDisplay = totalItems ?? liveProducts.length;

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-10 pt-28 sm:px-8 lg:px-16">
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
              {isLoading ? (
                <span>Đang tải dữ liệu...</span>
              ) : apiError ? (
                <span className="text-red-500">{apiError}</span>
              ) : (
                <span>
                  {countDisplay} vật phẩm
                  {query ? ` phù hợp với "${query}"` : ""}
                </span>
              )}
            </p>
          </header>

          {popularProducts === null ? (
            <div className="relative mb-10 flex h-64 w-full items-center justify-center overflow-hidden rounded-[32px] border border-white/30 bg-white/10 shadow-glass backdrop-blur-md sm:h-80">
              <div className="size-10 animate-spin rounded-full border-4 border-white/20 border-t-aqua"></div>
            </div>
          ) : popularProducts.length > 0 ? (
            <FeaturedSlider items={popularProducts} />
          ) : null}

          <MerchToolbar
            activeFilter={activeFilter}
            activeCategory={activeCategory}
            categoryOptions={categoryOptions.length > 0 ? categoryOptions : undefined}
            filterOptions={FILTER_OPTIONS}
            onFilterChange={(v) => { setActiveFilter(v); setPage(1); }}
            onCategoryChange={(v) => { setActiveCategory(v); setPage(1); }}
            onQueryChange={(v) => { setQuery(v); setPage(1); }}
            query={query}
          />

          <div className={isLoading ? "opacity-50 transition-opacity" : ""}>
            {!isLoading && apiError ? (
              <div className="flex flex-col items-center justify-center py-24 text-red-400">
                <p className="font-sans text-base">{apiError}</p>
              </div>
            ) : (
              <ProductGrid products={liveProducts} />
            )}
          </div>

          <Pagination
            currentPage={page}
            onPageChange={setPage}
            totalPages={pagesDisplay}
          />
        </GlassContainer>
      </div>
    </main>
  );
}