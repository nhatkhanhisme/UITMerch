export type ApiResponse<T> = {
  data: T;
  message: string;
  status: number;
};

export type Category = {
  id: string;
  name: string;
  slug?: string;
};

export type Product = {
  id: string;
  name: string;
  description?: string;
  price?: number;
  imageUrl?: string;
};

export type OrderItem = {
  id: string;
  merchId: string;
  merchName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
};

export type Order = {
  id: string;
  items: OrderItem[];
  totalAmount: number;
  status: string;
};

export type User = {
  id: string;
  email: string;
  fullName: string;
};
