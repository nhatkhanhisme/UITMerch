import type { ButtonHTMLAttributes, ReactNode } from "react";
import type { ButtonBaseProps } from "../../types/ui";

interface ButtonProps
  extends ButtonBaseProps,
    ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
}

const variantClasses = {
  primary:
    "bg-peach text-black-blue shadow-glass hover:bg-gold disabled:bg-dark-gray disabled:text-gray",
  secondary:
    "bg-aqua text-black-blue shadow-glass hover:bg-gold disabled:bg-dark-gray disabled:text-gray",
  outline:
    "border border-peach bg-transparent text-black-blue hover:bg-peach disabled:border-dark-gray disabled:text-gray"
};

const sizeClasses = {
  sm: "h-9 px-4 text-sm",
  md: "h-11 px-6 text-base",
  lg: "h-14 px-8 text-lg"
};

export function Button({
  children,
  className = "",
  disabled = false,
  loading = false,
  size = "md",
  type = "button",
  variant = "primary",
  ...props
}: ButtonProps) {
  const isDisabled = disabled || loading;

  return (
    <button
      className={[
        "inline-flex items-center justify-center gap-2 rounded-glass font-sans font-semibold leading-none transition duration-200",
        "focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aqua",
        "disabled:cursor-not-allowed disabled:shadow-none",
        variantClasses[variant],
        sizeClasses[size],
        className
      ].join(" ")}
      disabled={isDisabled}
      type={type}
      {...props}
    >
      {loading ? (
        <span
          aria-hidden="true"
          className="size-4 animate-spin rounded-full border-2 border-black-blue border-t-transparent"
        />
      ) : null}
      <span>{children}</span>
    </button>
  );
}
