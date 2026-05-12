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
        "min-h-[80vh] w-full max-w-canvas mx-auto",
        "rounded-[40px] border border-white/60",
        "bg-white/[0.04]backdrop-blur-lg",
        "shadow-[0_8px_32px_rgba(82,128,145,0.15)]",
        "p-6 sm:p-10",
        className,
      ].join(" ")}
    >
      {children}
    </div>
  );
}
