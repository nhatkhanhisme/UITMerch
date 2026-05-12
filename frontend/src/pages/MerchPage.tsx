import { lazy, Suspense, useMemo, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { FeaturedSlider } from "../components/features/FeaturedSlider";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { ProductGrid } from "../components/features/ProductGrid";
import { FEATURED_PRODUCTS, MOCK_PRODUCTS } from "../mocks/merchData";

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

  const processed = useMemo(() => {
    let list = MOCK_PRODUCTS.filter(
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
  }, [query, activeFilter]);

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

          <FeaturedSlider items={FEATURED_PRODUCTS} />

          <MerchToolbar
            activeFilter={activeFilter}
            filterOptions={FILTER_OPTIONS}
            onFilterChange={handleFilterChange}
            onQueryChange={handleQueryChange}
            query={query}
          />

          <ProductGrid products={paginated} />

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