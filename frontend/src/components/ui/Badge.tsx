import type { HTMLAttributes, ReactNode } from "react";
import type { BadgeBaseProps } from "../../types/ui";

interface BadgeProps extends BadgeBaseProps, HTMLAttributes<HTMLSpanElement> {
  children: ReactNode;
}

const variantClasses = {
  peach: "border-peach bg-peach/20 text-black-blue",
  gold: "border-gold bg-gold/20 text-black-blue",
  aqua: "border-aqua bg-aqua/20 text-black-blue"
};

export function Badge({
  children,
  className = "",
  variant = "peach",
  ...props
}: BadgeProps) {
  return (
    <span
      className={[
        "inline-flex h-8 items-center rounded-glass border px-4 font-sans text-sm font-semibold leading-none shadow-glass",
        variantClasses[variant],
        className
      ].join(" ")}
      {...props}
    >
      {children}
    </span>
  );
}
