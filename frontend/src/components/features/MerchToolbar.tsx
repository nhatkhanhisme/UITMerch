import { useEffect, useRef, useState } from "react";

export interface FilterOption {
  label: string;
  value: string;
}

interface MerchToolbarProps {
  query?: string;
  activeFilter: string | null;
  filterOptions: FilterOption[];
  onQueryChange?: (value: string) => void;
  onFilterChange: (value: string | null) => void;
  activeCategory?: string | null;
  categoryOptions?: FilterOption[];
  onCategoryChange?: (value: string | null) => void;
}

function SearchIcon() {
  return (
    <svg aria-hidden="true" className="size-5 shrink-0 text-ink/40" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <circle cx="11" cy="11" r="8" />
      <path d="m21 21-4.35-4.35" strokeLinecap="round" />
    </svg>
  );
}

function ChevronDownIcon({ open }: { open: boolean }) {
  return (
    <svg
      aria-hidden="true"
      className={`size-4 transition-transform duration-200 ${open ? "rotate-180" : ""}`}
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      viewBox="0 0 24 24"
    >
      <path d="m6 9 6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function Dropdown({
  label,
  isActive,
  options,
  activeValue,
  onSelect,
  resetLabel,
  zIndex = "z-40",
}: {
  label: string;
  isActive: boolean;
  options: FilterOption[];
  activeValue: string | null | undefined;
  onSelect: (value: string) => void;
  resetLabel?: string;
  zIndex?: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [isOpen]);

  return (
    <div className={`relative ${zIndex}`} ref={ref}>
      <button
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        className={[
          "flex items-center gap-2 rounded-full border px-5 py-3",
          "text-sm font-semibold backdrop-blur-md transition whitespace-nowrap",
          isActive
            ? "border-aqua bg-aqua/20 text-black-blue shadow-inner"
            : "border-white/60 bg-white/50 text-ink hover:bg-white/75",
        ].join(" ")}
        onClick={() => setIsOpen((o) => !o)}
        type="button"
      >
        <span>{label}</span>
        <ChevronDownIcon open={isOpen} />
      </button>

      {isOpen && (
        <div
          className={[
            "absolute right-0 top-[calc(100%+6px)] min-w-[180px] max-h-[300px] overflow-y-auto",
            "rounded-[20px] border border-white/60 bg-white/90 backdrop-blur-2xl",
            "shadow-[0_8px_32px_rgba(82,128,145,0.18)]",
            "animate-in fade-in slide-in-from-top-2 duration-150 scrollbar-hide",
          ].join(" ")}
          role="listbox"
        >
          {resetLabel && (
            <button
              className={[
                "flex w-full items-center justify-between px-5 py-3 font-sans text-sm transition",
                !activeValue ? "bg-aqua/25 font-semibold text-black-blue" : "text-ink hover:bg-white/60",
              ].join(" ")}
              onClick={() => { onSelect(""); setIsOpen(false); }}
              role="option"
              type="button"
            >
              <span>{resetLabel}</span>
              {!activeValue && <span className="text-xs">✓</span>}
            </button>
          )}
          {options.map((opt) => (
            <button
              className={[
                "flex w-full items-center justify-between px-5 py-3 font-sans text-sm transition",
                opt.value === activeValue ? "bg-aqua/25 font-semibold text-black-blue" : "text-ink hover:bg-white/60",
              ].join(" ")}
              key={opt.value}
              onClick={() => { onSelect(opt.value); setIsOpen(false); }}
              role="option"
              type="button"
            >
              <span>{opt.label}</span>
              {opt.value === activeValue && <span className="text-xs">✓</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

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
  const activeSortLabel = filterOptions.find((o) => o.value === activeFilter)?.label ?? "Sắp xếp";
  const activeCatLabel = categoryOptions?.find((o) => o.value === activeCategory)?.label ?? "Tất cả danh mục";

  return (
    <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center">
      {onQueryChange && (
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
            onChange={(e) => onQueryChange(e.target.value)}
            placeholder="Tìm kiếm..."
            type="text"
            value={query ?? ""}
          />
          {(query?.length ?? 0) > 0 && (
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
      )}

      {categoryOptions && onCategoryChange && (
        <Dropdown
          activeValue={activeCategory}
          isActive={!!activeCategory}
          label={activeCatLabel}
          onSelect={(v) => onCategoryChange(v || null)}
          options={categoryOptions}
          resetLabel="Tất cả danh mục"
          zIndex="z-40"
        />
      )}

      <Dropdown
        activeValue={activeFilter}
        isActive={!!activeFilter}
        label={activeSortLabel}
        onSelect={(v) => onFilterChange(v || null)}
        options={filterOptions}
        zIndex="z-50"
      />
    </div>
  );
}
