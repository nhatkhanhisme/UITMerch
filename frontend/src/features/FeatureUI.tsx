import type { ReactNode } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { featureError } from "../lib/featureUtils";
import { useAuthStore } from "../stores/authStore";
export function FeatureFrame({
  title,
  children,
  customer = false,
}: {
  title: string;
  children: ReactNode;
  customer?: boolean;
}) {
  const user = useAuthStore((s) => s.user);
  const location = useLocation();
  if (customer && !user)
    return <Navigate to="/auth" replace state={{ from: location.pathname }} />;
  if (customer && user?.role !== "CUSTOMER")
    return (
      <main className="feature-page">
        <h1>Chức năng dành cho khách hàng</h1>
        <Link to="/">Về trang chủ</Link>
      </main>
    );
  return (
    <main className="feature-page">
      <h1>{title}</h1>
      {children}
    </main>
  );
}
export function FeatureError({
  error,
  retry,
}: {
  error: unknown;
  retry?: () => void;
}) {
  return error ? (
    <div role="alert" className="feature-error">
      {featureError(error)}{" "}
      {retry && (
        <button type="button" onClick={retry}>
          Thử lại
        </button>
      )}
    </div>
  ) : null;
}
export function FeatureLoading() {
  return (
    <p role="status" className="p-4">
      Đang tải…
    </p>
  );
}
export function PageControls({
  page,
  total,
  change,
}: {
  page: number;
  total: number;
  change: (p: number) => void;
}) {
  return total > 1 ? (
    <nav aria-label="Phân trang" className="feature-actions">
      <button disabled={page === 0} onClick={() => change(page - 1)}>
        Trang trước
      </button>
      <span>
        {page + 1}/{total}
      </span>
      <button disabled={page + 1 >= total} onClick={() => change(page + 1)}>
        Trang sau
      </button>
    </nav>
  ) : null;
}
