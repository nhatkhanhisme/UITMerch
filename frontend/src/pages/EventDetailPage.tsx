import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { getPublicEvent } from "../api/event";
import { getPublicOrganizationDetail } from "../api/organization";
import { getApiErrorMessage } from "../api/auth";
import type { EventResponse } from "../types/shared";
import { CalendarDays, Building2, ArrowLeft, ArrowRight, Package } from "lucide-react";
import { dateTime, money } from "../lib/featureUtils";

function getStatusBadge(event: EventResponse) {
  let label = ({ ENDED: "Đã kết thúc", CANCELLED: "Đã hủy", DRAFT: "Nháp" } as Record<string, string>)[event.status];
  if (event.status === "PUBLISHED") {
    const parse = (value?: string) => value ? Date.parse(/[Zz]|[+-]\d\d:\d\d$/.test(value) ? value : value + "+07:00") : NaN;
    const start = parse(event.startsAt), end = parse(event.endsAt);
    label = Number.isFinite(end) && end >= start && end < Date.now() ? "Đã kết thúc" : start > Date.now() ? "Sắp diễn ra" : "Đã công bố";
    if (start <= Date.now() && end >= Date.now()) label = "Đang diễn ra";
  }
  return <span className="event-state">{label || "Thông tin sự kiện"}</span>;
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
        setError(null);
        setOrgName("Đang cập nhật");
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
      } catch (err: unknown) {
        if (isActive) {
          setError(getApiErrorMessage(err, "Không thể tải chi tiết sự kiện."));
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

  return (
    <main className="customer-event-page relative min-h-screen bg-canvas px-4 pb-16 pt-28 sm:px-8">
      <AmbientBackgroundGradients />
      <div className="relative z-10 mx-auto max-w-6xl">
        <Link to="/events" className="order-back-link mb-6"><ArrowLeft aria-hidden="true" size={18} /> Danh sách sự kiện</Link>
        {isLoading ? <section className="order-card" role="status">Đang tải sự kiện…</section> : error || !eventData ? (
          <section className="order-card" role="alert"><h1>Không thể mở sự kiện</h1><p className="panel-description">{error || "Sự kiện không tồn tại."}</p></section>
        ) : (
          <div className="event-detail-layout">
            <article className="order-card event-article">
              {eventData.coverUrl && <div className="event-cover"><img src={eventData.coverUrl} alt={eventData.title} /></div>}
              <div className="event-article-body">
                {getStatusBadge(eventData)}
                <h1>{eventData.title}</h1>
                <dl className="event-facts">
                  <div><CalendarDays aria-hidden="true" size={20} /><div><dt>Bắt đầu</dt><dd>{eventData.startsAt ? dateTime(eventData.startsAt) : "Đang cập nhật"}</dd></div></div>
                  <div><CalendarDays aria-hidden="true" size={20} /><div><dt>Kết thúc</dt><dd>{eventData.endsAt && (!eventData.startsAt || Date.parse(eventData.endsAt) >= Date.parse(eventData.startsAt)) ? dateTime(eventData.endsAt) : "Đang cập nhật"}</dd></div></div>
                  <div><Building2 aria-hidden="true" size={20} /><div><dt>Đơn vị tổ chức</dt><dd><Link to={`/organization/${eventData.orgId}`}>{orgName}</Link></dd></div></div>
                </dl>
                <section className="event-description"><h2>Về sự kiện</h2><p>{eventData.description || "Nội dung sự kiện đang được cập nhật."}</p></section>
              </div>
            </article>
            <aside className="event-merch-panel order-card" aria-label="Vật phẩm sự kiện">
              <div className="panel-heading"><Package aria-hidden="true" size={22} /><h2>Vật phẩm sự kiện</h2></div>
              <p className="panel-description">Khám phá các vật phẩm gắn với sự kiện này.</p>
              {eventData.merch?.length ? eventData.merch.map((item) => (
                <Link className="event-merch-item" key={item.id} to={`/merch/${item.id}`}>
                  {item.images?.[0] ? <img src={item.images[0]} alt="" /> : <span className="event-merch-placeholder"><Package aria-hidden="true" /></span>}
                  <div><h3>{item.name}</h3><p>{item.price === 0 ? "Miễn phí" : item.price == null ? "Đang cập nhật giá" : money(item.price)}</p><span>Xem vật phẩm <ArrowRight aria-hidden="true" size={14} /></span></div>
                </Link>
              )) : <p className="panel-description">Chưa có vật phẩm liên kết.</p>}
            </aside>
          </div>
        )}
      </div>
    </main>
  );
}
