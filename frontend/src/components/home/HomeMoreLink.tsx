import { Link } from "react-router-dom";

interface HomeMoreLinkProps {
  ariaLabel: string;
  to: string;
}

export function HomeMoreLink({ ariaLabel, to }: HomeMoreLinkProps) {
  return (
    <Link
      aria-label={ariaLabel}
      className="
        group inline-flex min-h-[64px] items-center gap-5 rounded-full
        border border-white/80 bg-white/35 px-8 py-4
        font-sans text-2xl font-semibold text-peach
        shadow-[0_18px_42px_rgba(82,128,145,0.18),inset_2px_2px_10px_rgba(255,255,255,0.95)]
        backdrop-blur-md
        transition duration-200 ease-out
        hover:-translate-y-0.5 hover:bg-white/45
      "
      to={to}
    >
      <span>Xem thêm</span>
      <span className="relative h-6 w-12" aria-hidden="true">
        <span className="absolute left-0 top-1/2 h-1 w-11 -translate-y-1/2 rounded-full bg-peach transition duration-200 group-hover:translate-x-1" />
        <span className="absolute right-0 top-1/2 h-5 w-5 -translate-y-1/2 rotate-45 border-r-4 border-t-4 border-peach transition duration-200 group-hover:translate-x-1" />
      </span>
    </Link>
  );
}
