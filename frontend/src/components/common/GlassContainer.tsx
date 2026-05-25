import type { ReactNode } from "react";

// ─── GlassContainer ──────────────────────────────────────────────────────────
// A reusable glassmorphism wrapper panel used across pages.

interface GlassContainerProps {
  children: ReactNode;
  className?: string;
}

export function GlassContainer({ children, className = "" }: GlassContainerProps) {
  return (
    <div
      className={[
        "min-h-0 w-full max-w-canvas mx-auto min-w-0",
        "sm:min-h-[80vh]",
        "rounded-[24px] border border-white/60 sm:rounded-[32px] lg:rounded-[40px]",
        "bg-white/[0.08] backdrop-blur-lg",
        "shadow-[0_8px_32px_rgba(82,128,145,0.15)]",
        "p-4 sm:p-6 lg:p-10",
        className,
      ].join(" ")}
    >
      {children}
    </div>
  );
}
