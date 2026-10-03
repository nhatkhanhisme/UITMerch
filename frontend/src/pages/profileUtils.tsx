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
    <div className="profile-info-row">
      <div className="flex items-start gap-3">
        {leading ? (
          <div aria-hidden="true" className="profile-info-icon">
            {leading}
          </div>
        ) : null}
        <div className="min-w-0">
          <p className="text-sm font-semibold text-slate">{label}</p>
          <p className="mt-1 break-words font-sans text-base font-semibold leading-relaxed text-black-blue">
            {value && value.trim().length > 0 ? value : "Chưa cập nhật"}
          </p>
          {description ? (
            <p className="mt-1 font-sans text-sm leading-relaxed text-gray">
              {description}
            </p>
          ) : null}
        </div>
      </div>
    </div>
  );
}
