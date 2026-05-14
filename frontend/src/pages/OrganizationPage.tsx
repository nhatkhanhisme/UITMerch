import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { OrgCard } from "../components/features/OrgCard";
import { getCatalogOrganizations } from "../api/catalog";
import type { MockOrganization } from "../mocks/orgData";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const PAGE_SIZE = 8;

const FILTER_OPTIONS: FilterOption[] = [
  { label: "A -> Z", value: "az" },
  { label: "Z -> A", value: "za" },
];

export function OrganizationPage() {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [organizations, setOrganizations] = useState<MockOrganization[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    const loadOrganizations = async () => {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const response = await getCatalogOrganizations();

        if (!isActive) {
          return;
        }

        setOrganizations(response.items);
      } catch {
        if (!isActive) {
          return;
        }

        setOrganizations([]);
        setErrorMessage(
          "Chua tai duoc danh sach to chuc tu backend. Ban co the bat VITE_USE_MOCK=true de dung du lieu demo.",
        );
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    };

    void loadOrganizations();

    return () => {
      isActive = false;
    };
  }, []);

  const processed = useMemo(() => {
    const q = query.toLowerCase();
    let list = organizations.filter(
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
  }, [query, activeFilter, organizations]);

  const totalPages = Math.max(1, Math.ceil(processed.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const paginated = processed.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
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
              To Chuc
            </h1>
            <p className="mt-1 font-sans text-sm text-ink/60">
              {processed.length} to chuc
              {query ? ` phu hop voi "${query}"` : ""}
            </p>
          </header>

          <MerchToolbar
            activeFilter={activeFilter}
            filterOptions={FILTER_OPTIONS}
            onFilterChange={handleFilterChange}
            onQueryChange={handleQueryChange}
            query={query}
          />

          {isLoading ? (
            <div className="mb-8 rounded-[28px] border border-white/50 bg-white/40 p-6 text-center font-sans text-sm text-ink/60 shadow-glass">
              Dang tai to chuc tu backend...
            </div>
          ) : null}

          {!isLoading && errorMessage ? (
            <div className="mb-8 rounded-[28px] border border-peach bg-peach/15 p-6 font-sans text-sm leading-6 text-black-blue">
              {errorMessage}
            </div>
          ) : null}

          {paginated.length > 0 ? (
            <div className="grid grid-cols-2 gap-8 sm:grid-cols-3 sm:gap-10 md:grid-cols-4">
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
              <p className="font-sans text-base">
                {errorMessage
                  ? "Chua co to chuc de hien thi."
                  : "Khong tim thay to chuc nao."}
              </p>
            </div>
          )}

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
