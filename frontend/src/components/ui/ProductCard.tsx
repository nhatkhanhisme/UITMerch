import type { HTMLAttributes } from "react";
import { Link } from "react-router-dom";

export interface ProductCardProps extends HTMLAttributes<HTMLElement> {
  name: string;
  image: string | null;
  orgName: string;
  description?: string;
  price?: number;
  detailPath?: string;
  category?: string;
  layout?: "vertical" | "horizontal";
}

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency",
});

export function ProductCard({
  className = "",
  description,
  detailPath,
  image,
  name,
  orgName,
  price,
  category,
  layout = "vertical",
  ...props
}: ProductCardProps) {
  const isHorizontal = layout === "horizontal";

  const content = (
    <article
      className={[
        "group flex h-full min-w-0 overflow-hidden rounded-[22px] border border-white/40 bg-white/20 p-3 sm:rounded-panel sm:p-4",
        "shadow-glass transition duration-300",
        "hover:-translate-y-1 hover:border-aqua hover:bg-white/30 hover:shadow-glass-inset",
        isHorizontal ? "flex-row items-center gap-4" : "flex-col",
        className,
      ].join(" ")}
      {...props}
    >
      <div 
        className={[
          "relative overflow-hidden rounded-[18px] border border-white/20 bg-white/5 shrink-0 sm:rounded-[28px]",
          isHorizontal ? "size-20 sm:size-28" : "aspect-[4/3] w-full sm:aspect-square"
        ].join(" ")}
      >
        {image ? (
          <img
            alt={name}
            className="size-full object-cover mix-blend-multiply transition duration-300 group-hover:scale-105"
            src={image}
          />
        ) : (
          <div className="flex size-full items-center justify-center bg-transparent text-4xl font-bold text-black-blue/10">
            {name.charAt(0)}
          </div>
        )}
        {category && !isHorizontal && (
          <div className="absolute right-1.5 top-1.5 max-w-[calc(100%-0.75rem)] rounded-full border border-white/40 bg-white/75 px-2 py-0.5 shadow-sm backdrop-blur-md sm:right-2 sm:top-2">
            <span className="block truncate text-[9px] font-bold uppercase tracking-wider text-black-blue/80 sm:text-[10px]">{category}</span>
          </div>
        )}
      </div>

      <div className={`flex min-w-0 flex-1 flex-col gap-1 ${isHorizontal ? "justify-center" : "mt-3 sm:mt-4"}`}>
        <span className="truncate font-sans text-[11px] font-medium text-ink/50 transition-colors group-hover:text-black-blue sm:text-xs">
          {orgName}
        </span>

        <h3 className="line-clamp-2 font-fredoka text-base font-bold leading-tight text-black-blue sm:text-xl">
          {name}
        </h3>

        {description ? (
          <p className="mt-1 line-clamp-2 font-sans text-xs text-ink/60">
            {description}
          </p>
        ) : null}

        <p className="mt-auto pt-2 font-sans text-xs font-semibold text-ink sm:pt-3 sm:text-sm">
          {price !== undefined && price > 0
            ? currencyFormatter.format(price)
            : "Sự kiện / Miễn phí"}
        </p>
      </div>
    </article>
  );

  if (!detailPath) {
    return content;
  }

  return (
    <Link
      aria-label={`Xem chi tiết ${name}`}
      className="block h-full rounded-panel focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-aqua"
      to={detailPath}
    >
      {content}
    </Link>
  );
}
