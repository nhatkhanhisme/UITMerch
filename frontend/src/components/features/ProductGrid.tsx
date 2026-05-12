import { ProductCard } from "../ui/ProductCard";
import type { MockProduct } from "../../mocks/merchData";

interface ProductGridProps {
  products: MockProduct[];
  emptyMessage?: string;
}

export function ProductGrid({
  emptyMessage = "Không tìm thấy vật phẩm nào.",
  products,
}: ProductGridProps) {
  if (products.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-ink/50">
        <svg
          aria-hidden="true"
          className="mb-4 size-12 opacity-30"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.5}
          viewBox="0 0 24 24"
        >
          <path
            d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
        <p className="font-google text-base">{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-4 sm:gap-6 md:grid-cols-3 lg:grid-cols-4">
      {products.map((product) => (
        <ProductCard
          category={product.category}
          id={`product-${product.id}`}
          image={product.image}
          key={product.id}
          name={product.name}
          price={product.price}
        />
      ))}
    </div>
  );
}
