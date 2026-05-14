import type { MockOrganization } from "../../mocks/orgData";
import { Link } from "react-router-dom";

// ─── OrgCard ──────────────────────────────────────────────────────────────────
// Circular Liquid Glass card specifically for Organizations.
// No description, no category, no price. Just logo + name.

interface OrgCardProps {
  org: MockOrganization;
}

export function OrgCard({ org }: OrgCardProps) {
  return (
    <Link
      aria-label={`Xem chi tiết ${org.name}`}
      className="flex flex-col items-center gap-4 rounded-panel focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-aqua"
      id={`org-${org.id}`}
      state={{ org }}
      to={`/organization/${org.id}`}
    >
      {/* Liquid glass circle strictly aspect-square and rounded-full */}
      <div
        className={[
          "relative aspect-square w-full overflow-hidden rounded-full",
          "border border-white/50 bg-white/20 backdrop-blur-md",
          "shadow-[inset_2px_2px_10px_rgba(255,255,255,0.6),0_4px_20px_rgba(82,128,145,0.12)]",
          "transition duration-300 hover:-translate-y-1 hover:shadow-[inset_2px_2px_16px_rgba(255,255,255,0.8),0_8px_32px_rgba(82,128,145,0.2)]",
        ].join(" ")}
      >
        {org.logo ? (
          <img
            alt={org.name}
            className="absolute inset-0 m-auto size-3/5 object-contain mix-blend-multiply opacity-90 transition duration-300 hover:scale-105 hover:opacity-100"
            src={org.logo}
          />
        ) : (
          <div className="flex size-full items-center justify-center bg-transparent text-5xl font-bold text-black-blue/20">
            {org.shortName.charAt(0)}
          </div>
        )}
      </div>

      {/* Org name below */}
      <p className="text-center font-fredoka text-base font-bold leading-tight text-black-blue">
        {org.name}
      </p>
    </Link>
  );
}
