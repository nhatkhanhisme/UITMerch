import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { FeaturedSlider } from "../components/features/FeaturedSlider";
import type { FeaturedItem } from "../components/features/FeaturedSlider";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { ProductGrid } from "../components/features/ProductGrid";
import { FEATURED_PRODUCTS, MOCK_PRODUCTS } from "../mocks/merchData";
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

  // Live API State
  const [liveProducts, setLiveProducts] = useState<MockProduct[]>([]);
  const [popularProducts, setPopularProducts] = useState<FeaturedItem[]>(FEATURED_PRODUCTS);
  const [categoryOptions, setCategoryOptions] = useState<FilterOption[]>([]);
  const [totalItems, setTotalItems] = useState<number | null>(null);
  const [serverTotalPages, setServerTotalPages] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [hasApiError, setHasApiError] = useState(false);

  // Load live data with debounce/effects
  useEffect(() => {
    let isActive = true;
    setIsLoading(true);

    const timer = window.setTimeout(() => {
      async function fetchData() {
        try {
          // Fetch org map
          const orgsRes = await getPublicOrganizations({ size: 50 });
          const orgMap: Record<string, string> = {};
          if (orgsRes?.data) {
            orgsRes.data.forEach((o) => {
              orgMap[o.id] = o.name;
            });
          }

          // Fetch Categories
          try {
            const catRes = await getCategories();
            if (isActive && catRes?.data) {
              setCategoryOptions(
                catRes.data.map((cat) => ({
                  label: cat.name,
                  value: cat.slug, // Use slug for filtering
                }))
              );
            }
          } catch {
            // keep empty categories
          }

          // Fetch Popular
          try {
            const popRes = await getPopularMerch();
            if (isActive && popRes?.data && popRes.data.length > 0) {
              const mappedFeatured = popRes.data.slice(0, 5).map((item) => ({
                id: item.id,
                name: item.name,
                orgName: orgMap[item.orgId] || "Tổ chức UIT",
                desc: item.description,
                image: item.imageUrl || null,
                link: `/merch/${item.id}`,
              }));
              setPopularProducts(mappedFeatured);
            }
          } catch {
            // Keep local FEATURED_PRODUCTS
          }

          // Fetch Merch List
          const res = await getPublicMerchList({
            keyword: query || undefined,
            category: activeCategory || undefined,
            page: page - 1,
            size: PAGE_SIZE,
            sort: activeFilter || undefined,
          });

          if (isActive && res?.data) {
            const mapped = res.data.map((item) =>
              mapMerchToMockProduct(item, orgMap),
            );
            setLiveProducts(mapped);
            setHasApiError(false);

            if (res.meta) {
              setTotalItems(res.meta.totalElements);
              setServerTotalPages(res.meta.totalPages);
            } else {
              setTotalItems(mapped.length);
              setServerTotalPages(Math.ceil(mapped.length / PAGE_SIZE));
            }
          }
        } catch {
          if (isActive) {
            setHasApiError(true);
          }
        } finally {
          if (isActive) {
            setIsLoading(false);
          }
        }
      }

      void fetchData();
    }, 300);

    return () => {
      isActive = false;
      window.clearTimeout(timer);
    };
  }, [query, activeFilter, activeCategory, page]);

  // Fallback local processed list if API fails or is not available
  const localProcessed = useMemo(() => {
    let list = MOCK_PRODUCTS.filter((p) => {
      const matchQuery =
        p.name.toLowerCase().includes(query.toLowerCase()) ||
        p.orgName.toLowerCase().includes(query.toLowerCase()) ||
        p.category.toLowerCase().includes(query.toLowerCase());
      
      const matchCategory = activeCategory 
        ? p.category.toLowerCase() === activeCategory.toLowerCase() // Simple fallback comparison
        : true;
        
      return matchQuery && matchCategory;
    });

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
  }, [query, activeFilter, activeCategory]);

  const localTotalPages = Math.max(1, Math.ceil(localProcessed.length / PAGE_SIZE));
  const localPaginated = useMemo(() => {
    const currentPage = Math.min(page, localTotalPages);
    return localProcessed.slice(
      (currentPage - 1) * PAGE_SIZE,
      currentPage * PAGE_SIZE,
    );
  }, [localProcessed, page, localTotalPages]);

  // Decide whether to show live products or fall back cleanly
  const displayedProducts = hasApiError ? localPaginated : liveProducts;
  const countDisplay = hasApiError
    ? localProcessed.length
    : totalItems ?? liveProducts.length;
  const pagesDisplay = hasApiError
    ? localTotalPages
    : serverTotalPages ?? (Math.ceil(liveProducts.length / PAGE_SIZE) || 1);

  const handleQueryChange = (value: string) => {
    setQuery(value);
    setPage(1);
  };

  const handleFilterChange = (value: string | null) => {
    setActiveFilter(value);
    setPage(1);
  };

  const handleCategoryChange = (value: string | null) => {
    setActiveCategory(value);
    setPage(1);
  };

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
              ) : (
                <span>
                  {countDisplay} vật phẩm
                  {query ? ` phù hợp với "${query}"` : ""}
                </span>
              )}
            </p>
          </header>

          <FeaturedSlider items={popularProducts} />

          <MerchToolbar
            activeFilter={activeFilter}
            activeCategory={activeCategory}
            categoryOptions={categoryOptions.length > 0 ? categoryOptions : undefined}
            filterOptions={FILTER_OPTIONS}
            onFilterChange={handleFilterChange}
            onCategoryChange={handleCategoryChange}
            onQueryChange={handleQueryChange}
            query={query}
          />

          <div className={isLoading ? "opacity-50 transition-opacity" : ""}>
            <ProductGrid products={displayedProducts} />
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