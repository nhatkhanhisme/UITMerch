import { create } from "zustand";

export type ToastVariant = "success" | "error" | "info";

export type Toast = {
  id: string;
  message: string;
  variant: ToastVariant;
  duration?: number;
};

type ToastState = {
  toasts: Toast[];
  addToast: (toast: Omit<Toast, "id">) => void;
  removeToast: (id: string) => void;
};

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  addToast: (toastItem) => {
    const id = typeof crypto !== "undefined" && "randomUUID" in crypto
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

    set((state) => ({
      toasts: [...state.toasts, { ...toastItem, id }],
    }));
  },
  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    }));
  },
}));

export const toast = {
  success: (message: string, duration = 4000) => {
    useToastStore.getState().addToast({ message, variant: "success", duration });
  },
  error: (message: string, duration = 5000) => {
    useToastStore.getState().addToast({ message, variant: "error", duration });
  },
  info: (message: string, duration = 4000) => {
    useToastStore.getState().addToast({ message, variant: "info", duration });
  },
};
