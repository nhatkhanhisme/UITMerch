import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { ProductGrid } from "../components/features/ProductGrid";
import { MOCK_PRODUCTS } from "../mocks/merchData";
import type { MockProduct } from "../mocks/merchData";
import { findOrganizationById } from "../mocks/orgData";
import type { MockOrganization } from "../mocks/orgData";
import {
  getPublicOrganizationDetail,
  getPublicOrganizations,
  getPublicOrgEvents,
  getPublicOrgMerch,
} from "../api/organization";
import {
  mapMerchToMockProduct,
  mapOrgToMockOrganization,
} from "../types/shared";
import type { EventResponse } from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const PAGE_SIZE = 8;

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

function OrganizationNotFound() {
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
          Tổ chức này chưa có trong UITMerch
        </h1>
        <p className="mt-4 max-w-xl text-sm leading-7 text-ink/65">
          Có thể đường dẫn đã thay đổi hoặc tổ chức chưa được đăng tải. Quay lại
          danh sách tổ chức để xem các đội nhóm, câu lạc bộ đang có.
        </p>
        <Link
          className="mt-8 rounded-full bg-black-blue px-8 py-3 text-sm font-bold text-white transition hover:-translate-y-0.5 hover:bg-ink"
          to="/organization"
        >
          Quay về tổ chức
        </Link>
      </section>
    </main>
  );
}

export function OrganizationDetailPage() {
  const { id } = useParams();
  const location = useLocation();
  const locationOrg = location.state?.org as MockOrganization | undefined;
  const fallbackOrg = findOrganizationById(id);

  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [activeTab, setActiveTab] = useState<"merch" | "events">("merch");

  // Live States
  const [liveOrg, setLiveOrg] = useState<MockOrganization | null>(locationOrg || null);
  const [liveEvents, setLiveEvents] = useState<EventResponse[]>([]);
  const [isEventsLoading, setIsEventsLoading] = useState(false);
  const [liveProducts, setLiveProducts] = useState<MockProduct[]>([]);
  const [totalItems, setTotalItems] = useState<number | null>(null);
  const [serverTotalPages, setServerTotalPages] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [hasApiError, setHasApiError] = useState(false);

  useEffect(() => {
    if (!id) {
      return;
    }

    let isActive = true;
    setIsLoading(true);

    async function fetchDetail() {
      try {
        let fetchedOrg: MockOrganization | null = null;
        try {
          const orgRes = await getPublicOrganizationDetail(id as string);
          if (orgRes?.data) {
            fetchedOrg = mapOrgToMockOrganization(orgRes.data);
          }
        } catch {
          try {
            const listRes = await getPublicOrganizations({ size: 50 });
            const matched = listRes?.data?.find((o) => o.id === id);
            if (matched) {
              fetchedOrg = mapOrgToMockOrganization(matched);
            }
          } catch {
            // retain fallback/state org
          }
        }

        if (isActive && fetchedOrg) {
          setLiveOrg(fetchedOrg);
        }

        try {
          const merchRes = await getPublicOrgMerch(id as string, {
            page: page - 1,
            size: PAGE_SIZE,
            sort: activeFilter ? SORT_MAP[activeFilter] : undefined,
          });

          if (isActive) {
            if (merchRes?.data) {
              const orgMap = fetchedOrg ? { [fetchedOrg.id]: fetchedOrg.name } : undefined;
              let mapped = merchRes.data.map((item) =>
                mapMerchToMockProduct(item, orgMap),
              );

              if (query) {
                const q = query.toLowerCase();
                mapped = mapped.filter(
                  (p) =>
                    p.name.toLowerCase().includes(q) ||
                    p.category.toLowerCase().includes(q) ||
                    p.description.toLowerCase().includes(q),
                );
              }

              setLiveProducts(mapped);
              if (merchRes.meta) {
                setTotalItems(merchRes.meta.totalElements);
                setServerTotalPages(merchRes.meta.totalPages);
              } else {
                setTotalItems(mapped.length);
                setServerTotalPages(Math.ceil(mapped.length / PAGE_SIZE));
              }
            } else {
              setLiveProducts([]);
            }
            setHasApiError(false);
          }
        } catch {
          if (isActive) {
            setHasApiError(true);
          }
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    void fetchDetail();

    return () => {
      isActive = false;
    };
  }, [id, page, activeFilter, query]);

  useEffect(() => {
    if (!id) return;
    let isActive = true;
    setIsEventsLoading(true);
    getPublicOrgEvents(id, { size: 20 })
      .then((res) => {
        if (isActive) setLiveEvents(res?.data ?? []);
      })
      .catch(() => {})
      .finally(() => { if (isActive) setIsEventsLoading(false); });
    return () => { isActive = false; };
  }, [id]);

  const organization = liveOrg || fallbackOrg;

  // Fallback processed list if API missing/error
  const processed = useMemo(() => {
    if (!organization) {
      return [];
    }

    const q = query.toLowerCase();
    const orgNameSafe = organization.name || "";

    let list = MOCK_PRODUCTS.filter((product) => {
      const pOrgSafe = product.orgName || "";
      const belongsToOrganization = pOrgSafe === orgNameSafe;

      if (!belongsToOrganization) {
        return false;
      }

      return (
        (product.name || "").toLowerCase().includes(q) ||
        (product.category || "").toLowerCase().includes(q) ||
        (product.description || "").toLowerCase().includes(q)
      );
    });

    switch (activeFilter) {
      case "az":
        list = [...list].sort((a, b) => (a.name || "").localeCompare(b.name || "", "vi"));
        break;
      case "za":
        list = [...list].sort((a, b) => (b.name || "").localeCompare(a.name || "", "vi"));
        break;
      case "price-asc":
        list = [...list].sort((a, b) => (a.price ?? 0) - (b.price ?? 0));
        break;
      case "price-desc":
        list = [...list].sort((a, b) => (b.price ?? 0) - (a.price ?? 0));
        break;
    }

    return list;
  }, [activeFilter, organization, query]);

  if (!organization && !isLoading) {
    return <OrganizationNotFound />;
  }

  const localTotalPages = Math.max(1, Math.ceil(processed.length / PAGE_SIZE));
  const localPaginated = useMemo(() => {
    const currentPage = Math.min(page, localTotalPages);
    return processed.slice(
      (currentPage - 1) * PAGE_SIZE,
      currentPage * PAGE_SIZE,
    );
  }, [processed, page, localTotalPages]);

  const displayedProducts = hasApiError ? localPaginated : liveProducts;
  const countDisplay = hasApiError
    ? processed.length
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

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-10 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <div className="z-0">
          <ShaderBackground />
        </div>
      </Suspense>

      <div className="relative z-10">
        {organization ? (
          <GlassContainer>
            <section className="grid gap-8 border-b border-white/50 pb-10 lg:grid-cols-[minmax(220px,320px)_minmax(0,1fr)] lg:items-center">
              <div className="mx-auto flex aspect-square w-full max-w-[280px] items-center justify-center overflow-hidden rounded-full border border-white/60 bg-white/25 p-8 shadow-[inset_2px_2px_18px_rgba(255,255,255,0.72),0_16px_45px_rgba(82,128,145,0.16)] backdrop-blur-xl lg:mx-0">
                {organization.logo ? (
                  <img
                    alt={organization.name}
                    className="size-full object-contain mix-blend-multiply"
                    src={organization.logo}
                  />
                ) : (
                  <span className="font-fredoka text-6xl font-bold text-black-blue/20">
                    {(organization.name || "U").charAt(0)}
                  </span>
                )}
              </div>

              <div className="text-center lg:text-left">
                <Link
                  className="inline-flex items-center rounded-full border border-white/70 bg-white/55 px-5 py-2.5 text-sm font-bold text-black-blue shadow-glass-inset transition hover:-translate-y-0.5 hover:border-aqua hover:bg-white"
                  to="/organization"
                >
                  ← Tổ chức
                </Link>
                <h1 className="mt-5 font-fredoka text-4xl font-bold leading-tight text-black-blue sm:text-5xl">
                  {organization.name}
                </h1>
                <p className="mx-auto mt-4 max-w-3xl font-sans text-base leading-8 text-ink/65 lg:mx-0">
                  {organization.description || "Chưa có mô tả chi tiết cho tổ chức này."}
                </p>

                <div className="mt-7 flex flex-wrap gap-4">
                  <div className="min-w-[140px] rounded-[28px] border border-white/50 bg-white/35 p-5 text-left">
                    <p className="text-xs font-semibold uppercase text-ink/45">
                      Vật phẩm
                    </p>
                    <p className="mt-2 font-fredoka text-2xl font-bold text-black-blue">
                      {countDisplay}
                    </p>
                  </div>
                  <div className="min-w-[140px] rounded-[28px] border border-white/50 bg-white/35 p-5 text-left">
                    <p className="text-xs font-semibold uppercase text-ink/45">
                      Sự kiện
                    </p>
                    <p className="mt-2 font-fredoka text-2xl font-bold text-black-blue">
                      {liveEvents.length}
                    </p>
                  </div>
                </div>
              </div>
            </section>

            <section className="pt-9">
              {/* Tab selector */}
              <div className="mb-6 flex gap-2">
                {(["merch", "events"] as const).map((tab) => (
                  <button
                    className={[
                      "rounded-full px-5 py-2 text-sm font-semibold transition",
                      activeTab === tab
                        ? "bg-black-blue text-white shadow-[0_8px_20px_rgba(82,128,145,0.2)]"
                        : "border border-white/60 bg-white/50 text-gray hover:bg-white/80",
                    ].join(" ")}
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    type="button"
                  >
                    {tab === "merch" ? "Vật phẩm" : "Sự kiện"}
                  </button>
                ))}
              </div>

              {activeTab === "merch" ? (
                <>
                  <header className="mb-6">
                    <h2 className="font-fredoka text-3xl font-bold text-black-blue sm:text-4xl">
                      Vật phẩm đang mở bán
                    </h2>
                    <p className="mt-2 font-sans text-sm text-ink/60">
                      {isLoading ? (
                        <span>Đang tải danh sách vật phẩm...</span>
                      ) : (
                        <span>
                          {countDisplay} vật phẩm
                          {query ? ` phù hợp với "${query}"` : ""} từ{" "}
                          {organization.name}
                        </span>
                      )}
                    </p>
                  </header>

                  <MerchToolbar
                    activeFilter={activeFilter}
                    filterOptions={FILTER_OPTIONS}
                    onFilterChange={handleFilterChange}
                  />

                  <div className={isLoading ? "opacity-50 transition-opacity" : ""}>
                    <ProductGrid
                      emptyMessage="Không tìm thấy vật phẩm nào của tổ chức này."
                      products={displayedProducts}
                    />
                  </div>

                  <Pagination
                    currentPage={page}
                    onPageChange={setPage}
                    totalPages={pagesDisplay}
                  />
                </>
              ) : (
                <>
                  <header className="mb-6">
                    <h2 className="font-fredoka text-3xl font-bold text-black-blue sm:text-4xl">
                      Sự kiện
                    </h2>
                    <p className="mt-2 font-sans text-sm text-ink/60">
                      {isEventsLoading ? "Đang tải sự kiện..." : `${liveEvents.length} sự kiện từ ${organization.name}`}
                    </p>
                  </header>

                  {isEventsLoading ? (
                    <div className="py-16 text-center text-sm text-ink/50">Đang tải...</div>
                  ) : liveEvents.length === 0 ? (
                    <div className="py-16 text-center text-sm text-ink/50">Tổ chức chưa có sự kiện nào.</div>
                  ) : (
                    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                      {liveEvents.map((event) => (
                        <Link
                          className="group rounded-[28px] border border-white/50 bg-white/35 p-4 shadow-glass backdrop-blur transition hover:-translate-y-1 hover:border-aqua hover:bg-white/60"
                          key={event.id}
                          to={`/events/${event.id}`}
                        >
                          <div className="aspect-video overflow-hidden rounded-[20px] bg-white/40">
                            {event.coverUrl ? (
                              <img
                                alt={event.title}
                                className="size-full object-cover transition group-hover:scale-105"
                                src={event.coverUrl}
                              />
                            ) : (
                              <div className="flex size-full items-center justify-center text-3xl text-ink/20">
                                📅
                              </div>
                            )}
                          </div>
                          <div className="mt-3 px-1">
                            {event.status === "PUBLISHED" && (
                              <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-bold text-green-700 border border-green-300">
                                Đang diễn ra
                              </span>
                            )}
                            {event.status === "ENDED" && (
                              <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-bold text-gray-600 border border-gray-300">
                                Đã kết thúc
                              </span>
                            )}
                            <p className="mt-2 font-fredoka text-lg font-bold leading-tight text-black-blue line-clamp-2">
                              {event.title}
                            </p>
                            {event.startsAt && (
                              <p className="mt-1 text-xs text-ink/55">
                                {new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(event.startsAt))}
                                {event.endsAt ? ` – ${new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(event.endsAt))}` : ""}
                              </p>
                            )}
                          </div>
                        </Link>
                      ))}
                    </div>
                  )}
                </>
              )}
            </section>
          </GlassContainer>
        ) : (
          <div className="py-24 text-center">Đang tải chi tiết tổ chức...</div>
        )}
      </div>
    </main>
  );
}
