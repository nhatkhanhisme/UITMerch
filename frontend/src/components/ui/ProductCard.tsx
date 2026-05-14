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
  ...props
}: ProductCardProps) {
  const content = (
    <article
      className={[
        "group flex h-full flex-col overflow-hidden rounded-panel border border-white/40 bg-white/20 p-4",
        "shadow-glass transition duration-300",
        "hover:-translate-y-1 hover:border-aqua hover:bg-white/30 hover:shadow-glass-inset",
        className,
      ].join(" ")}
      {...props}
    >
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
        {category && (
          <div className="absolute top-2 right-2 rounded-full bg-white/70 backdrop-blur-md px-2 py-0.5 border border-white/40 shadow-sm">
            <span className="text-[10px] font-bold text-black-blue/80 uppercase tracking-wider">{category}</span>
          </div>
        )}
      </div>

      <div className="mt-4 flex flex-1 flex-col gap-1">
        <span className="font-sans text-xs font-medium text-ink/50 transition-colors group-hover:text-black-blue">
          {orgName}
        </span>

        <h3 className="font-fredoka text-xl font-bold leading-tight text-black-blue">
          {name}
        </h3>

        {description ? (
          <p className="line-clamp-2 mt-1 font-sans text-xs text-ink/60">
            {description}
          </p>
        ) : null}

        <p className="mt-auto pt-3 font-sans text-sm font-semibold text-ink">
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
