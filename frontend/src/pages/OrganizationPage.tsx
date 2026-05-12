import { lazy, Suspense, useMemo, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { OrgCard } from "../components/features/OrgCard";
import { MOCK_ORGANIZATIONS } from "../mocks/orgData";

// ─── Lazy-load ShaderBackground ───────────────────────────────────────────────
const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  }))
);

const PAGE_SIZE = 8;

const FILTER_OPTIONS: FilterOption[] = [
  { label: "A → Z", value: "az" },
  { label: "Z → A", value: "za" },
];

// ─── OrganizationPage ─────────────────────────────────────────────────────────
export function OrganizationPage() {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  const processed = useMemo(() => {
    const q = query.toLowerCase();
    let list = MOCK_ORGANIZATIONS.filter(
      (o) =>
        o.name.toLowerCase().includes(q) ||
        o.shortName.toLowerCase().includes(q) ||
        o.category.toLowerCase().includes(q)
    );

    if (activeFilter === "az") {
      list = [...list].sort((a, b) => a.name.localeCompare(b.name, "vi"));
    } else if (activeFilter === "za") {
      list = [...list].sort((a, b) => b.name.localeCompare(a.name, "vi"));
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
      {/* Shader Background (z-0, non-blocking) */}
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <div className="z-0">
          <ShaderBackground />
        </div>
      </Suspense>

      {/* Page content (z-10) */}
      <div className="relative z-10">
        <GlassContainer>
          {/* Header */}
          <header className="mb-6">
            <h1 className="font-brand text-3xl font-bold text-black-blue sm:text-4xl">
              Tổ Chức
            </h1>
            <p className="mt-1 font-google text-sm text-ink/60">
              {processed.length} tổ chức
              {query ? ` phù hợp với "${query}"` : ""}
            </p>
          </header>

          {/* Toolbar */}
          <MerchToolbar
            activeFilter={activeFilter}
            filterOptions={FILTER_OPTIONS}
            onFilterChange={handleFilterChange}
            onQueryChange={handleQueryChange}
            query={query}
          />

          {/* Organization Grid */}
          {paginated.length > 0 ? (
            <div className="grid grid-cols-2 gap-8 sm:gap-10 sm:grid-cols-3 md:grid-cols-4">
              {paginated.map((org) => (
                <OrgCard key={org.id} org={org} />
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-24 text-ink/50">
              <svg
                aria-hidden="true"
                className="mb-4 size-12 opacity-30"
                fill="none"
                stroke="currentColor"
                strokeWidth={1.5}
                viewBox="0 0 24 24"
              >
                <path
                  d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              <p className="font-google text-base">Không tìm thấy tổ chức nào.</p>
            </div>
          )}

          {/* Pagination */}
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