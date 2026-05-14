import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { GlassContainer } from "../components/common/GlassContainer";
import { Pagination } from "../components/common/Pagination";
import { MerchToolbar } from "../components/features/MerchToolbar";
import type { FilterOption } from "../components/features/MerchToolbar";
import { getPublicEvents } from "../api/event";
import { getPublicOrganizations } from "../api/organization";
import type { EventResponse } from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const PAGE_SIZE = 8;

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

const fallbackEvents: UIEventItem[] = [
  {
    bannerUrl:
      "https://placehold.co/800x400/e9feff/1a3a4a?font=montserrat&text=GDGoC+DevFest",
    description:
      "Sự kiện công nghệ thường niên lớn nhất dành cho cộng đồng lập trình viên và sinh viên đam mê công nghệ tại UIT.",
    endDate: "2026-11-20T17:00:00Z",
    id: "ev-1",
    location: "Hội trường E, Đại học CNTT",
    name: "GDGoC DevFest 2026",
    orgName: "GDGoC UIT",
    startDate: "2026-11-20T08:00:00Z",
    status: "UPCOMING",
  },
  {
    bannerUrl:
      "https://placehold.co/800x400/e9feff/1a3a4a?font=montserrat&text=UIT+Sport+Day",
    description:
      "Hội thao truyền thống sinh viên UIT với các bộ môn bóng đá, bóng rổ, cầu lông và cờ vua vô cùng kịch tính.",
    endDate: "2026-12-05T17:00:00Z",
    id: "ev-2",
    location: "Sân vận động UIT",
    name: "Hội Thao Sinh Viên UIT 2026",
    orgName: "Đoàn Hội UIT",
    startDate: "2026-12-01T07:00:00Z",
    status: "ONGOING",
  },
  {
    bannerUrl:
      "https://placehold.co/800x400/e9feff/1a3a4a?font=montserrat&text=IT+Job+Fair",
    description:
      "Ngày hội việc làm kết nối sinh viên UIT với hơn 50 doanh nghiệp công nghệ hàng đầu trong và ngoài nước.",
    endDate: "2026-10-15T16:00:00Z",
    id: "ev-3",
    location: "Sảnh C, Đại học CNTT",
    name: "UIT IT Job Fair 2026",
    orgName: "Khoa CNTT",
    startDate: "2026-10-15T08:00:00Z",
    status: "COMPLETED",
  },
];

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
    case "UPCOMING":
      return (
        <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-bold text-blue-700 border border-blue-300">
          Sắp diễn ra
        </span>
      );
    case "ONGOING":
      return (
        <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-bold text-green-700 border border-green-300 animate-pulse">
          Đang diễn ra
        </span>
      );
    case "COMPLETED":
      return (
        <span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-bold text-gray-600 border border-gray-300">
          Đã kết thúc
        </span>
      );
    default:
      return (
        <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-700 border border-amber-300">
          {status || "Thông tin"}
        </span>
      );
  }
}

export function EventPage() {
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  // Live States
  const [liveEvents, setLiveEvents] = useState<UIEventItem[]>([]);
  const [totalItems, setTotalItems] = useState<number | null>(null);
  const [serverTotalPages, setServerTotalPages] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [hasApiError, setHasApiError] = useState(false);

  useEffect(() => {
    let isActive = true;
    setIsLoading(true);

    const timer = window.setTimeout(() => {
      async function fetchEvents() {
        try {
          const orgsRes = await getPublicOrganizations({ size: 50 });
          const orgMap: Record<string, string> = {};
          if (orgsRes?.data) {
            orgsRes.data.forEach((o) => {
              orgMap[o.id] = o.name;
            });
          }

          const res = await getPublicEvents({
            page: page - 1,
            size: PAGE_SIZE,
            sort: activeFilter || undefined,
          });

          if (isActive && res?.data) {
            let mapped: UIEventItem[] = res.data.map((ev: EventResponse) => ({
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

            if (query) {
              const q = query.toLowerCase();
              mapped = mapped.filter(
                (ev) =>
                  ev.name.toLowerCase().includes(q) ||
                  ev.description.toLowerCase().includes(q) ||
                  ev.location.toLowerCase().includes(q) ||
                  ev.orgName.toLowerCase().includes(q),
              );
            }

            setLiveEvents(mapped);
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

      void fetchEvents();
    }, 300);

    return () => {
      isActive = false;
      window.clearTimeout(timer);
    };
  }, [page, activeFilter, query]);

  const processedFallback = useMemo(() => {
    const q = query.toLowerCase();
    return fallbackEvents.filter(
      (ev) =>
        ev.name.toLowerCase().includes(q) ||
        ev.description.toLowerCase().includes(q) ||
        ev.location.toLowerCase().includes(q) ||
        ev.orgName.toLowerCase().includes(q),
    );
  }, [query]);

  const localTotalPages = Math.max(1, Math.ceil(processedFallback.length / PAGE_SIZE));
  const localPaginated = useMemo(() => {
    const currentPage = Math.min(page, localTotalPages);
    return processedFallback.slice(
      (currentPage - 1) * PAGE_SIZE,
      currentPage * PAGE_SIZE,
    );
  }, [processedFallback, page, localTotalPages]);

  const displayedEvents = hasApiError ? localPaginated : liveEvents;
  const countDisplay = hasApiError
    ? processedFallback.length
    : totalItems ?? liveEvents.length;
  const pagesDisplay = hasApiError
    ? localTotalPages
    : serverTotalPages ?? (Math.ceil(liveEvents.length / PAGE_SIZE) || 1);

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
              Sự Kiện & Hoạt Động
            </h1>
            <p className="mt-1 font-sans text-sm text-ink/60">
              {isLoading ? (
                <span>Đang tải danh sách sự kiện...</span>
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
            onFilterChange={handleFilterChange}
            onQueryChange={handleQueryChange}
            query={query}
          />

          <div className={isLoading ? "opacity-50 transition-opacity" : ""}>
            {displayedEvents.length > 0 ? (
              <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
                {displayedEvents.map((ev) => (
                  <article
                    className="group flex flex-col overflow-hidden rounded-[32px] border border-white/60 bg-white/40 shadow-glass backdrop-blur-md transition duration-300 hover:-translate-y-1 hover:border-aqua hover:shadow-glass-inset"
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
                          <span className="text-base">📅</span>
                          <span>{formatDate(ev.startDate)}</span>
                        </div>
                        {ev.location && (
                          <div className="flex items-center gap-2">
                            <span className="text-base">📍</span>
                            <span className="truncate">{ev.location}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-24 text-ink/50">
                <p className="font-sans text-base">Không tìm thấy sự kiện nào.</p>
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
