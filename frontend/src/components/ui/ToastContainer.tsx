import { useEffect } from "react";
import { useToastStore, type Toast } from "../../stores/toastStore";

const variantStyles = {
  success: {
    wrapper: "border-aqua bg-white/90 shadow-[0_16px_40px_rgba(82,128,145,0.25)]",
    icon: "✓",
    iconBg: "bg-aqua/20 text-black-blue border-aqua/40",
  },
  error: {
    wrapper: "border-peach bg-white/90 shadow-[0_16px_40px_rgba(255,140,120,0.25)]",
    icon: "✕",
    iconBg: "bg-peach/20 text-black-blue border-peach/40",
  },
  info: {
    wrapper: "border-gold bg-white/90 shadow-[0_16px_40px_rgba(255,200,100,0.25)]",
    icon: "ℹ",
    iconBg: "bg-gold/20 text-black-blue border-gold/40",
  },
};

function ToastItem({ toast }: { toast: Toast }) {
  const removeToast = useToastStore((state) => state.removeToast);
  const styles = variantStyles[toast.variant];

  useEffect(() => {
    const timer = setTimeout(() => {
      removeToast(toast.id);
    }, toast.duration ?? 4000);

    return () => clearTimeout(timer);
  }, [toast.id, toast.duration, removeToast]);

  return (
    <div
      className={[
        "flex w-full max-w-sm items-center gap-3 rounded-glass border p-4 backdrop-blur-md transition-all duration-300 animate-in fade-in slide-in-from-top-4",
        styles.wrapper,
      ].join(" ")}
      role="alert"
    >
      <div className={`flex size-7 shrink-0 items-center justify-center rounded-full border font-sans text-xs font-bold ${styles.iconBg}`}>
        {styles.icon}
      </div>
      <p className="min-w-0 flex-1 font-sans text-sm font-semibold leading-snug text-black-blue">
        {toast.message}
      </p>
      <button
        aria-label="Close toast"
        className="flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-bold text-gray transition hover:bg-white/50 hover:text-black-blue"
        onClick={() => removeToast(toast.id)}
        type="button"
      >
        ✕
      </button>
    </div>
  );
}

export function ToastContainer() {
  const toasts = useToastStore((state) => state.toasts);

  if (toasts.length === 0) {
    return null;
  }

  return (
    <div className="fixed right-6 top-24 z-[9999] flex flex-col gap-3 pointer-events-none sm:top-28">
      {toasts.map((t) => (
        <div className="pointer-events-auto" key={t.id}>
          <ToastItem toast={t} />
        </div>
      ))}
    </div>
  );
}
