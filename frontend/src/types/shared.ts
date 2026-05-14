export type ButtonBaseProps = {
  variant?: "primary" | "secondary" | "outline";
  size?: "sm" | "md" | "lg";
  loading?: boolean;
};

export type InputBaseProps = {
  errorMessage?: string;
  label?: string;
  placeholder?: string;
  type?: string;
};

export type BadgeBaseProps = {
  variant?: "peach" | "gold" | "aqua";
};

export type OrderItem = {
  productId: string;
  quantity: number;
  color?: string;
  size?: string;
  price: number;
};

export type User = {
  id: string;
  email: string;
  fullName: string;
  role?: string;
};

export type Product = {
  id: string;
  name: string;
  price?: number;
  description?: string;
};

export type Category = {
  id: string;
  name: string;
  slug?: string;
};

export type Order = {
  id: string;
  userId: string;
  totalAmount: number;
  status: string;
};

export type ApiResponse<T> = {
  status?: number;
  success?: boolean;
  message: string;
  data: T;
  meta?: unknown;
};
