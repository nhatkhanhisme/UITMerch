import { useCallback, useEffect, useRef, useState } from "react";
import { searchMerchByImage } from "../../api/merch";
import { ProductCard } from "../ui/ProductCard";
import { mapMerchToMockProduct } from "../../types/shared";
import type { MerchWithSimilarity, VisualSearchResponse } from "../../types/shared";

const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_SIZE = 5 * 1024 * 1024;

type State =
  | { kind: "idle" }
  | { kind: "previewing"; file: File; preview: string }
  | { kind: "loading"; preview: string }
  | { kind: "done"; preview: string; result: VisualSearchResponse }
  | { kind: "error"; preview?: string; message: string };

function CameraIcon() {
  return (
    <svg className="size-10 text-ink/30" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M6.827 6.175A2.31 2.31 0 0 1 5.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 0 0-1.134-.175 2.31 2.31 0 0 1-1.64-1.055l-.822-1.316a2.192 2.192 0 0 0-1.736-1.039 48.774 48.774 0 0 0-5.232 0 2.192 2.192 0 0 0-1.736 1.039l-.821 1.316Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 12.75a4.5 4.5 0 1 1-9 0 4.5 4.5 0 0 1 9 0ZM18.75 10.5h.008v.008h-.008V10.5Z" />
    </svg>
  );
}

function SparkleIcon() {
  return (
    <svg className="size-4" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09Z" />
    </svg>
  );
}

interface VisualSearchModalProps {
  open: boolean;
  onClose: () => void;
}

export function VisualSearchModal({ open, onClose }: VisualSearchModalProps) {
  const [state, setState] = useState<State>({ kind: "idle" });
  const [dragOver, setDragOver] = useState(false);
  const [fileError, setFileError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Reset state when modal closes
  useEffect(() => {
    if (!open) {
      setState({ kind: "idle" });
      setFileError(null);
      setDragOver(false);
    }
  }, [open]);

  // Close on Escape
  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [open, onClose]);

  const validateAndPreview = (file: File): boolean => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      setFileError("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP.");
      return false;
    }
    if (file.size > MAX_SIZE) {
      setFileError("Ảnh không được vượt quá 5 MB.");
      return false;
    }
    setFileError(null);
    return true;
  };

  const handleFile = useCallback((file: File) => {
    if (!validateAndPreview(file)) return;
    const preview = URL.createObjectURL(file);
    setState({ kind: "previewing", file, preview });
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
    e.target.value = "";
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  };

  const handleSearch = async () => {
    if (state.kind !== "previewing") return;
    const { file, preview } = state;
    setState({ kind: "loading", preview });
    try {
      const res = await searchMerchByImage(file);
      setState({ kind: "done", preview, result: res.data });
    } catch (err: unknown) {
      const message =
        err && typeof err === "object" && "response" in err
          ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message ?? "Đã có lỗi xảy ra. Vui lòng thử lại.")
          : "Đã có lỗi xảy ra. Vui lòng thử lại.";
      setState({ kind: "error", message });
    }
  };

  const reset = () => {
    setState({ kind: "idle" });
    setFileError(null);
  };

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center p-3 sm:items-center sm:p-4"
      style={{ background: "rgba(10,30,40,0.35)", backdropFilter: "blur(6px)" }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="relative max-h-[92svh] w-full max-w-lg overflow-y-auto rounded-[24px] border border-white/40 bg-white/90 shadow-glass backdrop-blur-2xl scrollbar-hide sm:max-h-[88vh] sm:rounded-[32px]">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-white/30 px-4 pb-4 pt-5 sm:items-center sm:px-7 sm:pb-5 sm:pt-7">
          <div>
            <h2 className="font-fredoka text-xl font-bold text-black-blue sm:text-2xl">Tìm kiếm bằng ảnh</h2>
            <p className="mt-0.5 font-sans text-xs text-ink/50">AI sẽ nhận diện vật phẩm và gợi ý sản phẩm phù hợp</p>
          </div>
          <button
            aria-label="Đóng"
            className="ml-3 flex size-9 shrink-0 items-center justify-center rounded-full border border-white/40 bg-white/50 text-ink/50 transition hover:bg-white/80 hover:text-black-blue sm:ml-4"
            onClick={onClose}
            type="button"
          >
            ✕
          </button>
        </div>

        <div className="px-4 py-5 sm:px-7 sm:py-6">
          {/* IDLE */}
          {state.kind === "idle" && (
            <div className="flex flex-col items-center gap-4">
              <div
                className={[
                  "relative flex w-full cursor-pointer flex-col items-center justify-center gap-3 rounded-[20px] border-2 border-dashed p-6 transition sm:rounded-[24px] sm:p-10",
                  dragOver
                    ? "border-aqua bg-aqua/10 scale-[1.01]"
                    : "border-white/50 bg-white/30 hover:border-aqua/60 hover:bg-white/50",
                ].join(" ")}
                onClick={() => inputRef.current?.click()}
                onDragLeave={(e) => { e.preventDefault(); setDragOver(false); }}
                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                onDrop={handleDrop}
              >
                <CameraIcon />
                <div className="text-center">
                  <p className="font-fredoka text-lg font-semibold text-black-blue">Kéo & thả ảnh vào đây</p>
                  <p className="mt-1 font-sans text-xs text-ink/50">hoặc nhấn để chọn ảnh</p>
                </div>
                <p className="font-sans text-[11px] text-ink/30">JPEG · PNG · WebP · Tối đa 5 MB</p>
              </div>
              {fileError && (
                <p className="font-sans text-sm text-red-500">{fileError}</p>
              )}
              <input
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={handleInputChange}
                ref={inputRef}
                type="file"
              />
            </div>
          )}

          {/* PREVIEWING */}
          {state.kind === "previewing" && (
            <div className="flex flex-col items-center gap-5">
              <div className="relative overflow-hidden rounded-[20px] border border-white/40 shadow-sm">
                <img
                  alt="Ảnh đã chọn"
                  className="max-h-52 w-full object-cover"
                  src={state.preview}
                />
              </div>
              <div className="flex w-full flex-col gap-3">
                <button
                  className="flex w-full items-center justify-center gap-2 rounded-full bg-aqua px-6 py-3.5 font-fredoka text-base font-bold text-black-blue shadow-md transition hover:brightness-105 active:scale-95"
                  onClick={handleSearch}
                  type="button"
                >
                  <SparkleIcon />
                  Tìm kiếm bằng AI
                </button>
                <button
                  className="font-sans text-sm text-ink/50 transition hover:text-black-blue"
                  onClick={reset}
                  type="button"
                >
                  Đổi ảnh
                </button>
              </div>
            </div>
          )}

          {/* LOADING */}
          {state.kind === "loading" && (
            <div className="flex flex-col items-center gap-6 py-6">
              <div className="relative overflow-hidden rounded-[20px] border border-white/40 shadow-sm opacity-60">
                <img
                  alt="Ảnh đang phân tích"
                  className="max-h-40 w-full object-cover"
                  src={state.preview}
                />
                <div className="absolute inset-0 flex items-center justify-center bg-white/40 backdrop-blur-sm">
                  <div className="size-10 animate-spin rounded-full border-4 border-white/20 border-t-aqua" />
                </div>
              </div>
              <p className="font-sans text-sm text-ink/60">Đang phân tích ảnh bằng AI…</p>
            </div>
          )}

          {/* DONE */}
          {state.kind === "done" && (
            <DoneState
              onReset={reset}
              preview={state.preview}
              result={state.result}
            />
          )}

          {/* ERROR */}
          {state.kind === "error" && (
            <div className="flex flex-col items-center gap-5 py-4">
              <div className="flex size-16 items-center justify-center rounded-full border border-red-200 bg-red-50">
                <span className="text-2xl">😕</span>
              </div>
              <div className="text-center">
                <p className="font-fredoka text-lg font-semibold text-black-blue">Đã có lỗi xảy ra</p>
                <p className="mt-1 font-sans text-sm text-ink/60">{state.message}</p>
              </div>
              <button
                className="rounded-full border border-white/60 bg-white/50 px-6 py-2.5 font-sans text-sm font-semibold text-ink transition hover:bg-white/80"
                onClick={reset}
                type="button"
              >
                Thử lại
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function DoneState({
  result,
  preview,
  onReset,
}: {
  result: VisualSearchResponse;
  preview: string;
  onReset: () => void;
}) {
  const products = result.results.map((item: MerchWithSimilarity) =>
    mapMerchToMockProduct(item.merch)
  );

  return (
    <div className="flex flex-col gap-5">
      {/* Thumbnail + AI tag row */}
      <div className="flex items-start gap-4">
        <img
          alt="Ảnh đã tìm"
          className="size-16 shrink-0 rounded-[14px] border border-white/40 object-cover shadow-sm"
          src={preview}
        />
        <div className="flex-1 min-w-0">
          <div className="flex w-full min-w-0 items-center gap-1.5 rounded-full border border-aqua/40 bg-aqua/15 px-3 py-1.5 font-sans text-xs font-semibold text-black-blue">
            <SparkleIcon />
            <span className="truncate min-w-0">AI nhận ra: {result.aiDescription}</span>
          </div>
          <p className="mt-1.5 font-sans text-xs text-ink/50">
            {products.length > 0 ? `${products.length} vật phẩm phù hợp` : "Không tìm thấy vật phẩm"}
          </p>
        </div>
      </div>

      {/* Results */}
      {products.length > 0 ? (
        <div className="flex flex-col gap-3">
          {products.map((p) => (
            <ProductCard
              key={p.id}
              category={p.category}
              detailPath={`/merch/${p.id}`}
              image={p.image}
              layout="horizontal"
              name={p.name}
              orgName={p.orgName}
              price={p.price}
            />
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center gap-3 rounded-[20px] border border-white/30 bg-white/30 py-10">
          <span className="text-3xl">🔍</span>
          <p className="font-sans text-sm text-ink/60 text-center">
            Không tìm thấy vật phẩm phù hợp.<br />Thử ảnh khác nhé!
          </p>
        </div>
      )}

      <button
        className="mt-1 font-sans text-sm text-ink/40 transition hover:text-black-blue text-center"
        onClick={onReset}
        type="button"
      >
        Tìm kiếm ảnh khác
      </button>
    </div>
  );
}
