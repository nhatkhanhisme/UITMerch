import { ArrowUpRight, Ticket } from "lucide-react";
import { Link } from "react-router-dom";

/** An evergreen introduction: campaign availability is checked on the destination page. */
export function PreorderSpotlight({ compact = false }: { compact?: boolean }) {
  return (
    <section
      aria-label="Bộ sưu tập mở đặt trước"
      className={`relative isolate overflow-hidden rounded-3xl border border-amber-200/80 bg-gradient-to-br from-[#fff9eb] via-white/95 to-[#e5f8fa] text-black-blue shadow-[0_12px_32px_rgba(130,100,52,0.09)] ${compact ? "mb-6 flex flex-col gap-4 p-5 sm:mb-8 sm:flex-row sm:items-center sm:justify-between sm:px-6" : "mt-6 p-5 text-left sm:p-6"}`}
    >
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-6 -top-7 -z-10 size-36 rounded-full border-[18px] border-amber-100/60"
      />
      <div className="min-w-0">
        <p className="mb-2 flex items-center gap-2 text-[10px] font-bold uppercase tracking-[0.16em] text-amber-900">
          <Ticket aria-hidden="true" className="size-4 shrink-0" />
          Mở bán theo đợt
        </p>
        <h2
          className={`${compact ? "text-lg sm:text-xl" : "text-xl sm:text-2xl"} font-semibold leading-tight`}
        >
          Bộ sưu tập mở đặt trước
        </h2>
        <p className="mt-2 max-w-md text-sm leading-6 text-slate">
          {compact
            ? "Những mẫu được mở bán riêng theo từng đợt. Khám phá và giữ chỗ cho mẫu bạn thích."
            : "Khám phá những mẫu từ tổ chức UIT. Giữ chỗ cho mẫu bạn thích và cùng đạt mục tiêu số lượng của đợt mở bán."}
        </p>
      </div>
      <Link
        to="/campaigns"
        className={`inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-full bg-black-blue px-5 py-3 text-center text-xs font-semibold text-white transition-colors hover:bg-slate focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amber-700 focus-visible:ring-offset-2 ${compact ? "self-start sm:self-center" : "mt-4"}`}
      >
        Khám phá bộ sưu tập
        <ArrowUpRight aria-hidden="true" className="size-4 shrink-0" />
      </Link>
    </section>
  );
}
