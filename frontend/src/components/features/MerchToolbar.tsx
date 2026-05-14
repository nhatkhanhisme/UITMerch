import { useState } from "react";

// ─── Types ─────────────────────────────────────────────────────────────────────
export interface FilterOption {
  label: string;
  value: string;
}

interface MerchToolbarProps {
  query: string;
  activeFilter: string | null;
  filterOptions: FilterOption[];
  onQueryChange: (value: string) => void;
  onFilterChange: (value: string | null) => void;
  // Optional category filter
  activeCategory?: string | null;
  categoryOptions?: FilterOption[];
  onCategoryChange?: (value: string | null) => void;
}

// ─── Icons ─────────────────────────────────────────────────────────────────────
function SearchIcon() {
  return (
    <svg
      aria-hidden="true"
      className="size-5 shrink-0 text-ink/40"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      viewBox="0 0 24 24"
    >
      <circle cx="11" cy="11" r="8" />
      <path d="m21 21-4.35-4.35" strokeLinecap="round" />
    </svg>
  );
}

function ChevronDownIcon() {
  return (
    <svg
      aria-hidden="true"
      className="size-4 transition duration-200 group-hover:rotate-180"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      viewBox="0 0 24 24"
    >
      <path d="m6 9 6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

// ─── MerchToolbar ──────────────────────────────────────────────────────────────
export function MerchToolbar({
  activeFilter,
  filterOptions,
  onFilterChange,
  onQueryChange,
  query,
  activeCategory,
  categoryOptions,
  onCategoryChange,
}: MerchToolbarProps) {
  const [isSortOpen, setIsSortOpen] = useState(false);
  const [isCatOpen, setIsCatOpen] = useState(false);
  
  const activeSortLabel = filterOptions.find((o) => o.value === activeFilter)?.label ?? "Sắp xếp";
  const activeCatLabel = categoryOptions?.find((o) => o.value === activeCategory)?.label ?? "Tất cả danh mục";

  const handleSortClick = (value: string) => {
    onFilterChange(activeFilter === value ? null : value);
    setIsSortOpen(false);
  };

  const handleCatClick = (value: string) => {
    onCategoryChange?.(activeCategory === value ? null : value);
    setIsCatOpen(false);
  };

  return (
    <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center">
      {/* ── Search ── */}
      <label
        className={[
          "flex flex-1 cursor-text items-center gap-3",
          "rounded-full border border-white/60 bg-white/50 backdrop-blur-md",
          "px-4 py-3 shadow-sm transition",
          "focus-within:border-aqua focus-within:ring-2 focus-within:ring-aqua/25",
        ].join(" ")}
      >
        <SearchIcon />
        <input
          className="flex-1 bg-transparent text-sm text-black-blue placeholder:text-ink/40 outline-none"
          id="merch-search"
          onChange={(e) => onQueryChange(e.target.value)}
          placeholder="Tìm kiếm..."
          type="search"
          value={query}
        />
        {query.length > 0 && (
          <button
            aria-label="Xóa tìm kiếm"
            className="shrink-0 text-ink/30 transition hover:text-ink/70"
            onClick={() => onQueryChange("")}
            type="button"
          >
            ✕
          </button>
        )}
      </label>

      {/* ── Category Dropdown ── */}
      {categoryOptions && onCategoryChange && (
        <div
          className="group relative z-40"
          onMouseEnter={() => setIsCatOpen(true)}
          onMouseLeave={() => setIsCatOpen(false)}
        >
          <button
            aria-expanded={isCatOpen}
            aria-haspopup="menu"
            className={[
              "flex items-center gap-2 rounded-full border px-5 py-3",
              "text-sm font-semibold backdrop-blur-md transition whitespace-nowrap",
              activeCategory
                ? "border-aqua bg-aqua/20 text-black-blue shadow-inner"
                : "border-white/60 bg-white/50 text-ink hover:bg-white/75",
            ].join(" ")}
            type="button"
          >
            <span>{activeCatLabel}</span>
            <ChevronDownIcon />
          </button>

          <div className="absolute top-full right-0 h-2 w-full bg-transparent" />

          {isCatOpen && (
            <div
              className={[
                "absolute right-0 top-[calc(100%+8px)] min-w-[200px] max-h-[300px] overflow-y-auto",
                "rounded-[20px] border border-white/60 bg-white/80 backdrop-blur-2xl",
                "shadow-[0_8px_32px_rgba(82,128,145,0.15)]",
                "animate-in fade-in slide-in-from-top-2 duration-200 custom-scrollbar"
              ].join(" ")}
              role="menu"
            >
              {/* Reset category option */}
              <button
                className={[
                  "flex w-full items-center justify-between px-5 py-3",
                  "font-sans text-sm transition",
                  !activeCategory
                    ? "bg-aqua/30 font-semibold text-black-blue"
                    : "text-ink hover:bg-white/60",
                ].join(" ")}
                onClick={() => handleCatClick("")}
                role="menuitem"
                type="button"
              >
                <span>Tất cả danh mục</span>
                {!activeCategory && <span aria-hidden="true" className="text-xs">✓</span>}
              </button>

              {categoryOptions.map((option) => (
                <button
                  className={[
                    "flex w-full items-center justify-between px-5 py-3",
                    "font-sans text-sm transition",
                    option.value === activeCategory
                      ? "bg-aqua/30 font-semibold text-black-blue"
                      : "text-ink hover:bg-white/60",
                  ].join(" ")}
                  key={option.value}
                  onClick={() => handleCatClick(option.value)}
                  role="menuitem"
                  type="button"
                >
                  <span>{option.label}</span>
                  {option.value === activeCategory && (
                    <span aria-hidden="true" className="text-xs">✓</span>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ── Sort Dropdown ── */}
      <div
        className="group relative z-50"
        onMouseEnter={() => setIsSortOpen(true)}
        onMouseLeave={() => setIsSortOpen(false)}
      >
        <button
          aria-expanded={isSortOpen}
          aria-haspopup="menu"
          className={[
            "flex items-center gap-2 rounded-full border px-5 py-3",
            "text-sm font-semibold backdrop-blur-md transition whitespace-nowrap",
            activeFilter
              ? "border-aqua bg-aqua/20 text-black-blue shadow-inner"
              : "border-white/60 bg-white/50 text-ink hover:bg-white/75",
          ].join(" ")}
          onClick={() => setIsSortOpen((open) => !open)}
          type="button"
        >
          <span>{activeSortLabel}</span>
          <ChevronDownIcon />
        </button>

        {/* Cầu nối tàng hình để tránh rớt menu khi hover */}
        <div className="absolute top-full right-0 h-2 w-full bg-transparent" />

        {/* Dropdown panel */}
        {isSortOpen && (
          <div
            className={[
              "absolute right-0 top-[calc(100%+8px)] min-w-[160px]",
              "rounded-[20px] border border-white/60 bg-white/80 backdrop-blur-2xl",
              "shadow-[0_8px_32px_rgba(82,128,145,0.15)] overflow-hidden",
              "animate-in fade-in slide-in-from-top-2 duration-200"
            ].join(" ")}
            role="menu"
          >
            {filterOptions.map((option) => (
              <button
                className={[
                  "flex w-full items-center justify-between px-5 py-3",
                  "font-sans text-sm transition",
                  option.value === activeFilter
                    ? "bg-aqua/30 font-semibold text-black-blue"
                    : "text-ink hover:bg-white/60",
                ].join(" ")}
                id={`filter-option-${option.value}`}
                key={option.value}
                onClick={() => handleSortClick(option.value)}
                role="menuitem"
                type="button"
              >
                <span>{option.label}</span>
                {option.value === activeFilter && (
                  <span aria-hidden="true" className="text-xs">✓</span>
                )}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
