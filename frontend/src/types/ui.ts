export type ButtonVariant = "primary" | "secondary" | "outline";
export type ButtonSize = "sm" | "md" | "lg";

export type ButtonBaseProps = {
  loading?: boolean;
  size?: ButtonSize;
  variant?: ButtonVariant;
};

export type InputBaseProps = {
  errorMessage?: string;
  label?: string;
  placeholder?: string;
  type?: string;
};

export type BadgeVariant = "peach" | "gold" | "aqua";

export type BadgeBaseProps = {
  variant?: BadgeVariant;
};
