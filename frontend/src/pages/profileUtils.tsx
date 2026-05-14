import type { ReactNode } from "react";

export const formatDate = (value?: string | null) => {
  if (!value) {
    return "N/A";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "N/A";
  }

  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
};

export const getInitials = (fullName: string) => {
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "U";
  }

  const [first, last] =
    parts.length === 1 ? [parts[0], ""] : [parts[0], parts[parts.length - 1]];
  return `${first[0] ?? ""}${last[0] ?? ""}`.toUpperCase();
};

export const toOptionalValue = (value: string) => {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
};

type InfoRowProps = {
  label: string;
  value?: string | null;
  description?: string;
  leading?: ReactNode;
};

export function ProfileInfoRow({
  label,
  value,
  description,
  leading,
}: InfoRowProps) {
  return (
    <div className="rounded-3xl border border-white/70 bg-white/75 px-4 py-4 shadow-[0_12px_28px_rgba(82,128,145,0.12)] backdrop-blur-sm">
      <div className="flex items-start gap-3">
        {leading ? (
          <div className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-2xl bg-brand-gradient text-black-blue shadow-[0_12px_24px_rgba(82,128,145,0.16)]">
            {leading}
          </div>
        ) : null}
        <div className="min-w-0">
          <p className="font-sans text-[11px] uppercase tracking-[0.3em] text-slate/60">
            {label}
          </p>
          <p className="mt-2 break-words font-brand text-lg font-black text-black-blue sm:text-xl">
            {value && value.trim().length > 0 ? value : "N/A"}
          </p>
          {description ? (
            <p className="mt-1 font-sans text-xs text-gray">{description}</p>
          ) : null}
        </div>
      </div>
    </div>
  );
}
