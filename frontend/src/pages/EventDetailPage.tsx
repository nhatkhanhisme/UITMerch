import { useEffect, useState, lazy, Suspense } from "react";
import { useParams, Link } from "react-router-dom";
import { GlassContainer } from "../components/common/GlassContainer";
import { getPublicEvent } from "../api/event";
import { getPublicOrganizationDetail } from "../api/organization";
import type { EventResponse } from "../types/shared";
import { Calendar, MapPin, Building, ArrowLeft } from "lucide-react";
import { ProductCard } from "../components/ui/ProductCard";
import { mapMerchToMockProduct } from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

function formatDate(isoString?: string) {
  if (!isoString) return "Đang cập nhật";
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
        <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-bold text-blue-700 border border-blue-300 sm:px-4 sm:py-1.5 sm:text-sm">
          Sắp diễn ra
        </span>
      );
    case "ONGOING":
      return (
        <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-bold text-green-700 border border-green-300 animate-pulse sm:px-4 sm:py-1.5 sm:text-sm">
          Đang diễn ra
        </span>
      );
    case "COMPLETED":
      return (
        <span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-bold text-gray-600 border border-gray-300 sm:px-4 sm:py-1.5 sm:text-sm">
          Đã kết thúc
        </span>
      );
    default:
      return (
        <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-700 border border-amber-300 sm:px-4 sm:py-1.5 sm:text-sm">
          {status || "Thông tin"}
        </span>
      );
  }
}

export function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [eventData, setEventData] = useState<EventResponse | null>(null);
  const [orgName, setOrgName] = useState("Đang tải...");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    async function fetchDetail() {
      if (!id) return;
      try {
        setIsLoading(true);
        const res = await getPublicEvent(id);
        if (isActive && res?.data) {
          setEventData(res.data);
          
          if (res.data.orgId) {
            try {
              const orgRes = await getPublicOrganizationDetail(res.data.orgId);
              if (isActive && orgRes?.data) {
                setOrgName(orgRes.data.name);
              }
            } catch {
              if (isActive) setOrgName("Tổ chức UIT");
            }
          }
        } else {
          throw new Error("Không tìm thấy dữ liệu.");
        }
      } catch (err: any) {
        if (isActive) {
          setError(err.message || "Không thể tải chi tiết sự kiện.");
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
  }, [id]);

  if (isLoading) {
    return (
      <main className="relative flex min-h-screen items-center justify-center bg-transparent px-3 pb-8 pt-24 sm:px-6 sm:pb-10 sm:pt-28 lg:px-16">
        <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
          <div className="z-0">
            <ShaderBackground />
          </div>
        </Suspense>
        <div className="relative z-10 size-12 animate-spin rounded-full border-4 border-white/40 border-t-aqua shadow-glass"></div>
      </main>
    );
  }

  if (error || !eventData) {
    return (
      <main className="relative flex min-h-screen flex-col items-center justify-center bg-transparent px-3 pb-8 pt-24 sm:px-6 sm:pb-10 sm:pt-28 lg:px-16">
        <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
          <div className="z-0">
            <ShaderBackground />
          </div>
        </Suspense>
        <GlassContainer className="relative z-10 flex flex-col items-center p-6 text-center sm:p-12">
          <p className="text-xl font-bold text-red-500 mb-4">{error || "Sự kiện không tồn tại."}</p>
          <Link to="/events" className="text-aqua hover:underline font-bold">
            Trở về danh sách sự kiện
          </Link>
        </GlassContainer>
      </main>
    );
  }

  const bannerUrl = eventData.coverUrl || "https://placehold.co/1200x500/e9feff/1a3a4a?font=montserrat&text=EVENT+BANNER";
  const orgMap = { [eventData.orgId]: orgName };
  const merchItems = (eventData.merch || []).map((m) => mapMerchToMockProduct(m, orgMap));

  return (
    <main className="relative min-h-screen bg-transparent px-3 pb-8 pt-24 sm:px-6 sm:pb-10 sm:pt-28 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <div className="z-0">
          <ShaderBackground />
        </div>
      </Suspense>

      <div className="relative z-10 mx-auto max-w-7xl space-y-5 sm:space-y-8">
        <Link to="/events" className="inline-flex items-center gap-2 text-black-blue/70 hover:text-aqua font-bold transition-colors w-fit">
          <ArrowLeft className="size-5" />
          <span>Danh sách sự kiện</span>
        </Link>

        <div className="grid grid-cols-1 items-start gap-5 lg:grid-cols-10 lg:gap-8">
          <div className="space-y-5 lg:col-span-7 sm:space-y-8">
            <GlassContainer className="overflow-hidden p-0">
              <div className="relative h-48 w-full bg-white/20 sm:h-96">
                <img 
                  src={bannerUrl} 
                  alt={eventData.title} 
                  className="size-full object-cover mix-blend-multiply"
                />
                <div className="absolute right-3 top-3 z-10 rounded-full shadow-lg sm:right-6 sm:top-6">
                  {getStatusBadge(eventData.status)}
                </div>
              </div>

              <div className="p-4 sm:p-12">
                <h1 className="mb-4 font-fredoka text-3xl font-bold text-black-blue sm:mb-6 sm:text-5xl">
                  {eventData.title}
                </h1>

                <div className="mb-6 flex flex-col gap-3 border-b border-ink/10 pb-6 font-sans text-sm text-ink/80 sm:mb-8 sm:flex-row sm:flex-wrap sm:gap-6 sm:pb-8 sm:text-base">
                  <div className="flex items-center gap-2">
                    <Calendar className="size-5 text-aqua" />
                    <span>
                      <strong>Bắt đầu:</strong> {formatDate(eventData.startsAt)}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Calendar className="size-5 text-aqua opacity-60" />
                    <span>
                      <strong>Kết thúc:</strong> {formatDate(eventData.endsAt)}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <MapPin className="size-5 text-aqua" />
                    <span>
                      <strong>Địa điểm:</strong> Đại học Công nghệ Thông tin
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Building className="size-5 text-aqua" />
                    <span>
                      <strong>Tổ chức bởi:</strong> {orgName}
                    </span>
                  </div>
                </div>

                <div className="prose prose-lg max-w-none text-ink/80 font-sans leading-relaxed">
                  <p className="whitespace-pre-wrap">{eventData.description || "Chưa có thông tin chi tiết."}</p>
                </div>
              </div>
            </GlassContainer>
          </div>

          <div className="space-y-5 lg:sticky lg:top-28 lg:col-span-3 lg:space-y-6">
            {merchItems.length > 0 && (
              <div className="flex flex-col gap-6">
                <div className="flex items-center gap-4">
                  <h2 className="font-fredoka text-2xl font-bold text-black-blue">
                    Vật Phẩm
                  </h2>
                  <div className="h-px flex-1 bg-gradient-to-r from-aqua/50 to-transparent"></div>
                </div>
                
                <div className="flex flex-col gap-4">
                  {merchItems.map((product) => (
                    <ProductCard
                      key={product.id}
                      detailPath={`/merch/${product.id}`}
                      description={product.description}
                      id={`product-${product.id}`}
                      image={product.image}
                      name={product.name}
                      orgName={product.orgName}
                      price={product.price}
                      category={product.category}
                      layout="horizontal"
                      className="!h-auto"
                    />
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
