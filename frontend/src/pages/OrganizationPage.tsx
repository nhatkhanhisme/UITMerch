import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { OrgCard } from "../components/features/OrgCard";
import { MOCK_ORGANIZATIONS } from "../mocks/orgData";
import type { MockOrganization } from "../mocks/orgData";
import { getPublicOrganizations } from "../api/organization";
import { mapOrgToMockOrganization } from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const PAGE_SIZE = 8;

const FILTER_OPTIONS: FilterOption[] = [
  { label: "A → Z", value: "az" },
  { label: "Z → A", value: "za" },
];

export function OrganizationPage() {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  // Live API State
  const [liveOrgs, setLiveOrgs] = useState<MockOrganization[]>([]);
  const [totalItems, setTotalItems] = useState<number | null>(null);
  const [serverTotalPages, setServerTotalPages] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [hasApiError, setHasApiError] = useState(false);

  useEffect(() => {
    let isActive = true;
    setIsLoading(true);

    const timer = window.setTimeout(() => {
      async function fetchOrgs() {
        try {
          const res = await getPublicOrganizations({
            page: page - 1,
            size: PAGE_SIZE,
            sort: activeFilter || undefined,
          });

          if (isActive && res?.data) {
            let mapped = res.data.map(mapOrgToMockOrganization);

            // Optional custom search keyword filter locally if backend endpoint doesn't accept keyword param natively
            if (query) {
              const q = query.toLowerCase();
              mapped = mapped.filter(
                (o) =>
                  o.name.toLowerCase().includes(q) ||
                  o.shortName.toLowerCase().includes(q) ||
                  o.category.toLowerCase().includes(q),
              );
            }

            setLiveOrgs(mapped);
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

      void fetchOrgs();
    }, 300);

    return () => {
      isActive = false;
      window.clearTimeout(timer);
    };
  }, [page, activeFilter, query]);

  // Fallback local list
  const processed = useMemo(() => {
    const q = query.toLowerCase();
    let list = MOCK_ORGANIZATIONS.filter(
      (o) =>
        o.name.toLowerCase().includes(q) ||
        o.shortName.toLowerCase().includes(q) ||
        o.category.toLowerCase().includes(q),
    );

    if (activeFilter === "az") {
      list = [...list].sort((a, b) => a.name.localeCompare(b.name, "vi"));
    } else if (activeFilter === "za") {
      list = [...list].sort((a, b) => b.name.localeCompare(a.name, "vi"));
    }

    return list;
  }, [query, activeFilter]);

  const localTotalPages = Math.max(1, Math.ceil(processed.length / PAGE_SIZE));
  const localPaginated = useMemo(() => {
    const currentPage = Math.min(page, localTotalPages);
    return processed.slice(
      (currentPage - 1) * PAGE_SIZE,
      currentPage * PAGE_SIZE,
    );
  }, [processed, page, localTotalPages]);

  const displayedOrgs = hasApiError ? localPaginated : liveOrgs;
  const countDisplay = hasApiError
    ? processed.length
    : totalItems ?? liveOrgs.length;
  const pagesDisplay = hasApiError
    ? localTotalPages
    : serverTotalPages ?? (Math.ceil(liveOrgs.length / PAGE_SIZE) || 1);

  const handleQueryChange = (value: string) => {
    setQuery(value);
    setPage(1);
  };

  const handleFilterChange = (value: string | null) => {
    setActiveFilter(value);
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
          <header className="mb-6">
            <h1 className="font-fredoka text-3xl font-bold text-black-blue sm:text-4xl">
              Tổ Chức
            </h1>
            <p className="mt-1 font-sans text-sm text-ink/60">
              {isLoading ? (
                <span>Đang tải dữ liệu...</span>
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
            onFilterChange={handleFilterChange}
            onQueryChange={handleQueryChange}
            query={query}
          />

          <div className={isLoading ? "opacity-50 transition-opacity" : ""}>
            {displayedOrgs.length > 0 ? (
              <div className="grid grid-cols-2 gap-8 sm:grid-cols-3 sm:gap-10 md:grid-cols-4">
                {displayedOrgs.map((org) => (
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
                <p className="font-sans text-base">Không tìm thấy tổ chức nào.</p>
              </div>
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