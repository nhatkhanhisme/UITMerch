import { create } from "zustand";
import type { OrderItem } from "../types/commerce";

type CartState = {
  items: OrderItem[];
  setItems: (items: OrderItem[]) => void;
  clearCart: () => void;
};

export const useCartStore = create<CartState>((set) => ({
  items: [],
  setItems: (items) => set({ items }),
  clearCart: () => set({ items: [] })
}));
