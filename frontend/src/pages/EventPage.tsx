import { lazy, Suspense, useEffect, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { getPublicEvents } from "../api/event";
import { getPublicOrganizations } from "../api/organization";
import type { EventResponse } from "../types/shared";
import { Calendar, MapPin } from "lucide-react";
import { Link } from "react-router-dom";
import { cacheGet, cacheSet, cacheKey } from "../lib/sessionCache";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const PAGE_SIZE = 16;

// "newest" → default sort by createdAt,desc (no param needed)
// "upcoming" → status filter, NOT a sort param
const FILTER_OPTIONS: FilterOption[] = [
  { label: "Mới nhất", value: "newest" },
  { label: "Sắp diễn ra", value: "upcoming" },
];

type UIEventItem = {
  id: string;
  name: string;
  description: string;
  bannerUrl: string;
  startDate?: string;
  endDate?: string;
  location: string;
  orgName: string;
  status: string;
};

function formatDate(isoString?: string) {
  if (!isoString) {
    return "Đang cập nhật";
  }
  try {
    const date = new Date(isoString);
    return new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }).format(date);
  } catch {
    return isoString;
  }
}

function getStatusBadge(status?: string) {
  switch (status?.toUpperCase()) {
    case "PUBLISHED":
      return (
        <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-bold text-green-700 border border-green-300">
          Đang diễn ra
        </span>
      );
    case "ENDED":
      return (
        <span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-bold text-gray-600 border border-gray-300">
          Đã kết thúc
        </span>
      );
    default:
      return null;
  }
}

export function EventPage() {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  // allEvents holds the FULL list — enables search-all
  const [allEvents, setAllEvents] = useState<UIEventItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;
    setIsLoading(true);

    const timer = window.setTimeout(() => {
      async function fetchAllEvents() {
        const ck = cacheKey("all_events", { filter: activeFilter ?? "newest" });
        const cached = cacheGet<UIEventItem[]>(ck);
        if (cached) {
          if (isActive) {
            setAllEvents(cached);
            setApiError(null);
            setIsLoading(false);
          }
          return;
        }

        try {
          // Reuse shared org map from session cache
          const ORG_KEY = "org_map";
          let orgMap: Record<string, string> = cacheGet<Record<string, string>>(ORG_KEY) ?? {};
          if (Object.keys(orgMap).length === 0) {
            const orgsRes = await getPublicOrganizations({ size: 50 });
            if (orgsRes?.data) {
              orgsRes.data.forEach((o) => { orgMap[o.id] = o.name; });
              cacheSet(ORG_KEY, orgMap, 10 * 60 * 1000);
            }
          }

          const isStatusFilter = activeFilter === "upcoming";
          const res = await getPublicEvents({
            page: 0,
            size: 200, // fetch entire dataset in one shot
            sort: (!activeFilter || activeFilter === "newest") ? "createdAt,desc" : undefined,
            status: isStatusFilter ? "UPCOMING" : undefined,
          });

          if (isActive && res?.data) {
            const mapped: UIEventItem[] = res.data.map((ev: EventResponse) => ({
              bannerUrl:
                ev.coverUrl ||
                "https://placehold.co/800x400/e9feff/1a3a4a?font=montserrat&text=EVENT",
              description: ev.description || "Chưa có mô tả cụ thể.",
              endDate: ev.endsAt,
              id: ev.id,
              location: "Đại học Công nghệ Thông tin",
              name: ev.title || "Sự kiện UIT",
              orgName: orgMap[ev.orgId] || "Cộng đồng UIT",
              startDate: ev.startsAt,
              status: ev.status || "UPCOMING",
            }));
            setAllEvents(mapped);
            setApiError(null);
            cacheSet(ck, mapped, 5 * 60 * 1000);
          }
        } catch {
          if (isActive) {
            setApiError("Không thể tải danh sách sự kiện. Vui lòng thử lại.");
          }
        } finally {
          if (isActive) {
            setIsLoading(false);
          }
        }
      }

      void fetchAllEvents();
    }, 300);

    return () => {
      isActive = false;
      window.clearTimeout(timer);
    };
  }, [activeFilter]);

  // Reset to page 1 when query or filter changes
  useEffect(() => { setPage(1); }, [query, activeFilter]);

  // Search across ALL events (entire dataset)
  const filteredEvents = query.trim()
    ? allEvents.filter((ev) => ev.name.toLowerCase().includes(query.toLowerCase()))
    : allEvents;

  // Client-side pagination
  const totalPages = Math.ceil(filteredEvents.length / PAGE_SIZE) || 1;
  const pageEvents = filteredEvents.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  const countDisplay = filteredEvents.length;

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
              Sự Kiện &amp; Hoạt Động
            </h1>
            <p className="mt-1 font-sans text-sm text-ink/60">
              {isLoading ? (
                <span>Đang tải danh sách sự kiện...</span>
              ) : apiError ? (
                <span className="text-red-500">{apiError}</span>
              ) : (
                <span>
                  {countDisplay} sự kiện
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
            ) : pageEvents.length > 0 ? (
              <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
                {pageEvents.map((ev) => (
                  <Link
                    to={`/event/${ev.id}`}
                    className="group flex flex-col overflow-hidden rounded-[32px] border border-white/60 bg-white/40 shadow-glass backdrop-blur-md transition duration-300 hover:-translate-y-1 hover:border-aqua hover:shadow-glass-inset focus-visible:outline-aqua focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4"
                    key={ev.id}
                  >
                    <div className="relative h-48 w-full overflow-hidden bg-white/50">
                      <img
                        alt={ev.name}
                        className="size-full object-cover mix-blend-multiply transition duration-500 group-hover:scale-105"
                        src={ev.bannerUrl}
                      />
                      <div className="absolute right-4 top-4 z-10">
                        {getStatusBadge(ev.status)}
                      </div>
                    </div>

                    <div className="flex flex-1 flex-col p-6">
                      <p className="font-sans text-xs font-bold uppercase tracking-wider text-ink/45">
                        {ev.orgName}
                      </p>
                      <h3 className="mt-2 font-fredoka text-xl font-bold text-black-blue group-hover:text-aqua">
                        {ev.name}
                      </h3>
                      <p className="mt-3 flex-1 font-sans text-sm leading-6 text-ink/70 line-clamp-3">
                        {ev.description}
                      </p>

                      <div className="mt-6 border-t border-white/50 pt-4 space-y-2 font-sans text-xs text-ink/65">
                        <div className="flex items-center gap-2">
                          <Calendar className="size-4 shrink-0 text-black-blue/60" />
                          <span>{formatDate(ev.startDate)}</span>
                        </div>
                        {ev.location && (
                          <div className="flex items-center gap-2">
                            <MapPin className="size-4 shrink-0 text-black-blue/60" />
                            <span className="truncate">{ev.location}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            ) : !isLoading ? (
              <div className="flex flex-col items-center justify-center py-24 text-ink/50">
                <p className="font-sans text-base">Không tìm thấy sự kiện nào.</p>
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
