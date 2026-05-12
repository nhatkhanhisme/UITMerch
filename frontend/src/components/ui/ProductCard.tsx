import type { HTMLAttributes } from "react";
import { Link } from "react-router-dom";

// ─── Props ────────────────────────────────────────────────────────────────────
export interface ProductCardProps extends HTMLAttributes<HTMLElement> {
  name: string;
  image: string | null;
  orgName: string;
  description?: string;
  price?: number;
}

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency",
});

// ─── ProductCard ──────────────────────────────────────────────────────────────
export function ProductCard({
  className = "",
  description,
  image,
  name,
  orgName,
  price,
  ...props
}: ProductCardProps) {
  return (
    <article
      className={[
        "group flex flex-col overflow-hidden rounded-panel border border-white/40 bg-white/20 p-4",
        "shadow-glass transition duration-300",
        "hover:-translate-y-1 hover:border-aqua hover:bg-white/30 hover:shadow-glass-inset",
        className,
      ].join(" ")}
      {...props}
    >
      {/* Image Area - Transparent if no image */}
      <div className="relative aspect-square overflow-hidden rounded-[28px] border border-white/20 bg-white/5">
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
      </div>

      {/* Info */}
      <div className="mt-4 flex flex-col gap-1">
        {/* Org name with underline hover */}
        <Link
          className="font-sans text-xs font-medium text-ink/50 transition-colors hover:text-black-blue hover:underline"
          to="#"
        >
          {orgName}
        </Link>

        {/* Product name */}
        <h3 className="font-fredoka text-xl font-bold leading-tight tracking-[0.01em] text-black-blue">
          {name}
        </h3>

        {/* Description */}
        {description ? (
          <p className="line-clamp-2 mt-1 font-sans text-xs text-ink/60">
            {description}
          </p>
        ) : null}

        {/* Price or free tag */}
        <p className="mt-3 font-sans text-sm font-semibold text-ink">
          {price !== undefined && price > 0
            ? currencyFormatter.format(price)
            : "Sự kiện / Miễn phí"}
        </p>
      </div>
    </article>
  );
}
