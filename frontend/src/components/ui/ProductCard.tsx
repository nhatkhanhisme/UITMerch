import type { ProductCardBaseProps } from "@repo/shared";
import type { HTMLAttributes } from "react";
import { Badge } from "./Badge";

interface ProductCardProps
  extends ProductCardBaseProps,
    HTMLAttributes<HTMLElement> {}

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency"
});

export function ProductCard({
  category,
  className = "",
  image,
  name,
  price,
  ...props
}: ProductCardProps) {
  return (
    <article
      className={[
        "group overflow-hidden rounded-panel border border-white/40 bg-white/20 p-4 shadow-glass transition duration-300",
        "hover:-translate-y-1 hover:border-aqua hover:bg-white/30 hover:shadow-glass-inset",
        className
      ].join(" ")}
      {...props}
    >
      <div className="aspect-square overflow-hidden rounded-panel bg-canvas">
        <img
          alt={name}
          className="size-full object-cover transition duration-300 group-hover:scale-105"
          src={image}
        />
      </div>
      <div className="mt-4 flex items-start justify-between gap-4">
        <div className="min-w-0">
          <Badge variant="aqua">{category}</Badge>
          <h3 className="mt-3 truncate font-brand text-2xl font-black leading-tight text-black-blue">
            {name}
          </h3>
        </div>
        <p className="shrink-0 font-google text-base text-ink">
          {currencyFormatter.format(price)}
        </p>
      </div>
    </article>
  );
}
