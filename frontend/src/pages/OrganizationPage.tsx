import { lazy, Suspense, useEffect, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { OrgCard } from "../components/features/OrgCard";
import type { MockOrganization } from "../mocks/orgData";
import { getPublicOrganizations } from "../api/organization";
import { mapOrgToMockOrganization } from "../types/shared";
import { cacheGet, cacheSet, cacheKey } from "../lib/sessionCache";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const PAGE_SIZE = 16;

// Map UI filter values → Spring Pageable sort params
const SORT_MAP: Record<string, string> = {
  az: "name,asc",
  za: "name,desc",
};

const FILTER_OPTIONS: FilterOption[] = [
  { label: "A → Z", value: "az" },
  { label: "Z → A", value: "za" },
];

export function OrganizationPage() {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  // allOrgs holds the FULL list (not just current page) — enables search-all
  const [allOrgs, setAllOrgs] = useState<MockOrganization[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  // Fetch the full list for the active sort — cached per sort key
  useEffect(() => {
    let isActive = true;
    setIsLoading(true);

    const timer = window.setTimeout(() => {
      async function fetchAllOrgs() {
        const ck = cacheKey("all_orgs", { sort: activeFilter ? SORT_MAP[activeFilter] : undefined });
        const cached = cacheGet<MockOrganization[]>(ck);
        if (cached) {
          if (isActive) {
            setAllOrgs(cached);
            setApiError(null);
            setIsLoading(false);
          }
          return;
        }

        try {
          const res = await getPublicOrganizations({
            page: 0,
            size: 200, // fetch entire dataset in one shot
            sort: activeFilter ? SORT_MAP[activeFilter] : undefined,
          });

          if (isActive && res?.data) {
            const mapped = res.data.map(mapOrgToMockOrganization);
            setAllOrgs(mapped);
            setApiError(null);
            cacheSet(ck, mapped, 5 * 60 * 1000);
          }
        } catch {
          if (isActive) {
            setApiError("Không thể tải danh sách tổ chức. Vui lòng thử lại.");
          }
        } finally {
          if (isActive) {
            setIsLoading(false);
          }
        }
      }

      void fetchAllOrgs();
    }, 300);

    return () => {
      isActive = false;
      window.clearTimeout(timer);
    };
  }, [activeFilter]);

  // Reset to page 1 when query or sort changes
  useEffect(() => { setPage(1); }, [query, activeFilter]);

  // Search across ALL orgs (entire dataset, not just current page)
  const filteredOrgs = query.trim()
    ? allOrgs.filter((o) => o.name.toLowerCase().includes(query.toLowerCase()))
    : allOrgs;

  // Client-side pagination over the filtered result
  const totalPages = Math.ceil(filteredOrgs.length / PAGE_SIZE) || 1;
  const pageOrgs = filteredOrgs.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  const countDisplay = filteredOrgs.length;

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-10 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <div className="z-0">
          <ShaderBackground />
        </div>
      </Suspense>

      <div className="relative z-10">
        <GlassContainer>
          <header className="mb-6">
            <h1 className="font-fredoka text-3xl font-bold text-black-blue sm:text-4xl">
              Tổ Chức
            </h1>
            <p className="mt-1 font-sans text-sm text-ink/60">
              {isLoading ? (
                <span>Đang tải dữ liệu...</span>
              ) : apiError ? (
                <span className="text-red-500">{apiError}</span>
              ) : (
                <span>
                  {countDisplay} tổ chức
                  {query ? ` phù hợp với "${query}"` : ""}
                </span>
              )}
            </p>
          </header>

          <MerchToolbar
            activeFilter={activeFilter}
            filterOptions={FILTER_OPTIONS}
            onFilterChange={(v) => { setActiveFilter(v); }}
            onQueryChange={(v) => { setQuery(v); }}
            query={query}
          />

          <div className={isLoading ? "opacity-50 transition-opacity" : ""}>
            {!isLoading && apiError ? (
              <div className="flex flex-col items-center justify-center py-24 text-red-400">
                <p className="font-sans text-base">{apiError}</p>
              </div>
            ) : pageOrgs.length > 0 ? (
              <div className="grid grid-cols-2 gap-8 sm:grid-cols-3 sm:gap-10 md:grid-cols-4">
                {pageOrgs.map((org) => (
                  <OrgCard key={org.id} org={org} />
                ))}
              </div>
            ) : !isLoading ? (
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
                <p className="font-sans text-base">Không tìm thấy tổ chức nào.</p>
              </div>
            ) : null}
          </div>

          <Pagination
            currentPage={page}
            onPageChange={setPage}
            totalPages={totalPages}
          />
        </GlassContainer>
      </div>
    </main>
  );
}