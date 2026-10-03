import { useEffect, useRef, useState } from "react";
import { Navigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Users, Building2, Package, Search, ShieldCheck } from "lucide-react";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import {
  adminListOrders,
  adminListOrganizations,
  adminListUsers,
  adminSetUserActive,
  adminUpdateOrgStatus,
  adminUpdateUserRole,
} from "../api/admin";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import {
  FeatureError,
  FeatureLoading,
  PageControls,
} from "../features/FeatureUI";
import { money, dateTime } from "../lib/featureUtils";
import type {
  OrganizationResponse,
  UserSummaryResponse,
} from "../types/shared";

const ROLE_LABELS: Record<string, string> = {
  CUSTOMER: "Khách hàng",
  ORGANIZER: "Ban tổ chức",
  ADMIN: "Quản trị viên",
};
const ORG_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ duyệt",
  ACTIVE: "Hoạt động",
  INACTIVE: "Tạm ngừng",
};
const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  READY: "Sẵn sàng nhận",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

function ConfirmAdminAction({
  title,
  description,
  busy,
  error,
  close,
  confirm,
}: {
  title: string;
  description: string;
  busy: boolean;
  error?: unknown;
  close: () => void;
  confirm: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    ref.current?.showModal();
  }, []);
  return (
    <dialog
      ref={ref}
      className="admin-confirm-dialog"
      aria-labelledby="admin-confirm-title"
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) close();
      }}
    >
      <h2 id="admin-confirm-title">{title}</h2>
      <p>{description}</p>
      {!!error && <FeatureError error={error} />}
      <div className="feature-actions">
        <button disabled={busy} onClick={confirm} type="button">
          {busy ? "Đang xử lý…" : "Xác nhận"}
        </button>
        <button
          disabled={busy}
          onClick={close}
          type="button"
          className="secondary-action"
          autoFocus
        >
          Quay lại
        </button>
      </div>
    </dialog>
  );
}
function AdminFilters({
  label,
  values,
  active,
  change,
}: {
  label: string;
  values: Record<string, string>;
  active: string;
  change: (value: string) => void;
}) {
  return (
    <nav className="status-filters" aria-label={label}>
      {Object.entries(values).map(([value, text]) => (
        <button
          type="button"
          key={value}
          aria-pressed={active === value}
          className={active === value ? "selected" : ""}
          onClick={() => change(value)}
        >
          {text}
        </button>
      ))}
    </nav>
  );
}
function AdminSearch({
  value,
  change,
}: {
  value: string;
  change: (value: string) => void;
}) {
  return (
    <label className="admin-search">
      <Search aria-hidden="true" size={18} />
      <span className="sr-only">Tìm trong trang hiện tại</span>
      <input
        value={value}
        onChange={(e) => change(e.target.value)}
        placeholder="Tìm trong trang hiện tại…"
        type="search"
      />
    </label>
  );
}
function UsersTab() {
  const self = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const [role, setRole] = useState("");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [action, setAction] = useState<{
    user: UserSummaryResponse;
    role?: string;
    active?: boolean;
  } | null>(null);
  const q = useQuery({
    queryKey: ["admin-users", self?.id, role, page],
    queryFn: () =>
      adminListUsers({
        role: role || undefined,
        page,
        size: 20,
        sort: "createdAt,desc",
      }),
  });
  const m = useMutation({
    mutationFn: async (request: NonNullable<typeof action>) =>
      request.role
        ? adminUpdateUserRole(request.user.id, request.role)
        : adminSetUserActive(request.user.id, request.active!),
    onSuccess: async () => {
      toast.success("Đã cập nhật tài khoản.");
      setAction(null);
      await qc.invalidateQueries({ queryKey: ["admin-users", self?.id] });
    },
  });
  const rows =
    q.data?.data.filter((u) =>
      `${u.fullName} ${u.email}`
        .toLocaleLowerCase("vi")
        .includes(search.toLocaleLowerCase("vi")),
    ) ?? [];
  return (
    <section className="admin-section">
      <header className="admin-section-header">
        <div>
          <h2>Quản lý tài khoản</h2>
          <p>
            Khách hàng, người tổ chức và quản trị viên. Trạng thái hoạt động độc
            lập với xác minh email.
          </p>
        </div>
        <AdminSearch value={search} change={setSearch} />
      </header>
      <AdminFilters
        label="Lọc vai trò tài khoản"
        values={{ "": "Tất cả", ...ROLE_LABELS }}
        active={role}
        change={(value) => {
          setRole(value);
          setPage(0);
        }}
      />
      {q.isPending ? (
        <FeatureLoading />
      ) : q.isError ? (
        <FeatureError error={q.error} retry={() => q.refetch()} />
      ) : (
        <>
          <div className="admin-table-wrap">
            <table className="admin-table">
              <caption className="sr-only">Danh sách tài khoản</caption>
              <thead>
                <tr>
                  <th>Tài khoản</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th>Xác minh email</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <strong>{u.fullName}</strong>
                      <span className="admin-cell-detail">{u.email}</span>
                      <span className="admin-cell-detail">
                        {u.createdAt ? dateTime(u.createdAt) : ""}
                      </span>
                    </td>
                    <td>
                      <select
                        aria-label={`Vai trò của ${u.fullName}`}
                        value={u.role}
                        disabled={u.id === self?.id || m.isPending}
                        onChange={(e) => {
                          m.reset();
                          setAction({ user: u, role: e.target.value });
                        }}
                      >
                        {Object.entries(ROLE_LABELS).map(([value, label]) => (
                          <option value={value} key={value}>
                            {label}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <span
                        className={`admin-badge ${u.isActive ? "positive" : "muted"}`}
                      >
                        {u.isActive ? "Hoạt động" : "Đã vô hiệu hóa"}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`admin-badge ${u.isVerified ? "positive" : "warning"}`}
                      >
                        <ShieldCheck aria-hidden="true" size={14} />
                        {u.isVerified ? "Đã xác minh" : "Chưa xác minh"}
                      </span>
                    </td>
                    <td>
                      <button
                        type="button"
                        className={
                          u.isActive ? "danger-action" : "secondary-action"
                        }
                        disabled={u.id === self?.id || m.isPending}
                        onClick={() => {
                          m.reset();
                          setAction({ user: u, active: !u.isActive });
                        }}
                      >
                        {u.id === self?.id
                          ? "Tài khoản của bạn"
                          : u.isActive
                            ? "Vô hiệu hóa"
                            : "Kích hoạt"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!rows.length && (
              <p className="admin-empty">Không có tài khoản phù hợp.</p>
            )}
          </div>
          <div className="admin-list-footer">
            <span>
              {q.data?.meta?.totalElements ?? q.data?.data.length ?? 0} tài
              khoản · Trang {page + 1}
            </span>
            <PageControls
              page={page}
              total={q.data?.meta?.totalPages ?? 1}
              change={setPage}
            />
          </div>
        </>
      )}
      {action && (
        <ConfirmAdminAction
          title={
            action.role
              ? "Đổi vai trò tài khoản?"
              : action.active
                ? "Kích hoạt tài khoản?"
                : "Vô hiệu hóa tài khoản?"
          }
          description={`${action.user.fullName} (${action.user.email}). ${action.role ? `Vai trò mới: ${ROLE_LABELS[action.role]}. Phiên đăng nhập hiện có sẽ hết hiệu lực.` : action.active ? "Tài khoản có thể đăng nhập lại nếu email đã được xác minh. Thao tác này không xác minh email." : "Tài khoản sẽ không thể tiếp tục đăng nhập hoặc sử dụng phiên hiện tại."}`}
          busy={m.isPending}
          error={m.error}
          close={() => setAction(null)}
          confirm={() => m.mutate(action)}
        />
      )}
    </section>
  );
}
function OrgsTab() {
  const self = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [action, setAction] = useState<{
    org: OrganizationResponse;
    status: string;
  } | null>(null);
  const q = useQuery({
    queryKey: ["admin-orgs", self?.id, status, page],
    queryFn: () =>
      adminListOrganizations({
        status: status || undefined,
        page,
        size: 20,
        sort: "createdAt,desc",
      }),
  });
  const m = useMutation({
    mutationFn: (a: NonNullable<typeof action>) =>
      adminUpdateOrgStatus(a.org.id, a.status),
    onSuccess: async () => {
      setAction(null);
      toast.success("Đã cập nhật trạng thái tổ chức.");
      await qc.invalidateQueries({ queryKey: ["admin-orgs", self?.id] });
    },
  });
  const rows =
    q.data?.data.filter((o) =>
      o.name.toLocaleLowerCase("vi").includes(search.toLocaleLowerCase("vi")),
    ) ?? [];
  return (
    <section className="admin-section">
      <header className="admin-section-header">
        <div>
          <h2>Quản lý tổ chức</h2>
          <p>Duyệt đăng ký, tạm ngừng hoặc khôi phục hoạt động của tổ chức.</p>
        </div>
        <AdminSearch value={search} change={setSearch} />
      </header>
      <AdminFilters
        label="Lọc trạng thái tổ chức"
        values={{ "": "Tất cả", ...ORG_STATUS_LABELS }}
        active={status}
        change={(s) => {
          setStatus(s);
          setPage(0);
        }}
      />
      {q.isPending ? (
        <FeatureLoading />
      ) : q.isError ? (
        <FeatureError error={q.error} retry={() => q.refetch()} />
      ) : (
        <>
          <div className="admin-org-grid">
            {rows.map((org) => (
              <article className="admin-org-card" key={org.id}>
                <header>
                  {org.logoUrl ? (
                    <img src={org.logoUrl} alt="" />
                  ) : (
                    <Building2 aria-hidden="true" size={32} />
                  )}
                  <div>
                    <h3>{org.name}</h3>
                    <span
                      className={`admin-badge ${org.status === "ACTIVE" ? "positive" : org.status === "PENDING" ? "warning" : "muted"}`}
                    >
                      {ORG_STATUS_LABELS[org.status] ?? org.status}
                    </span>
                  </div>
                </header>
                <p>{org.description || "Chưa có giới thiệu tổ chức."}</p>
                <p className="admin-cell-detail">
                  {org.totalMerch ?? 0} vật phẩm đang bán ·{" "}
                  {org.createdAt ? dateTime(org.createdAt) : ""}
                </p>
                <footer>
                  {org.status !== "ACTIVE" && (
                    <button
                      className="feature-button"
                      disabled={m.isPending}
                      type="button"
                      onClick={() => {
                        m.reset();
                        setAction({ org, status: "ACTIVE" });
                      }}
                    >
                      {org.status === "PENDING"
                        ? "Duyệt tổ chức"
                        : "Khôi phục hoạt động"}
                    </button>
                  )}
                  {org.status !== "INACTIVE" && (
                    <button
                      className="danger-action"
                      disabled={m.isPending}
                      type="button"
                      onClick={() => {
                        m.reset();
                        setAction({ org, status: "INACTIVE" });
                      }}
                    >
                      {org.status === "PENDING"
                        ? "Từ chối đăng ký"
                        : "Tạm ngừng hoạt động"}
                    </button>
                  )}
                </footer>
              </article>
            ))}
          </div>
          {!rows.length && (
            <p className="admin-empty">Không có tổ chức phù hợp.</p>
          )}
          <div className="admin-list-footer">
            <span>{q.data?.meta?.totalElements ?? 0} tổ chức</span>
            <PageControls
              page={page}
              total={q.data?.meta?.totalPages ?? 1}
              change={setPage}
            />
          </div>
        </>
      )}
      {action && (
        <ConfirmAdminAction
          title={
            action.status === "ACTIVE"
              ? "Cho phép tổ chức hoạt động?"
              : "Tạm ngừng tổ chức?"
          }
          description={`${action.org.name}. ${action.status === "ACTIVE" ? "Tổ chức có thể quản lý và mở bán vật phẩm. Những vật phẩm đã bị ẩn cần được công bố lại." : "Vật phẩm đang công bố sẽ bị ẩn khỏi trang mua sắm. Các đơn hàng hiện có vẫn được lưu."}`}
          busy={m.isPending}
          error={m.error}
          close={() => setAction(null)}
          confirm={() => m.mutate(action)}
        />
      )}
    </section>
  );
}
function AdminOrdersTab() {
  const self = useAuthStore((s) => s.user);
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const q = useQuery({
    queryKey: ["admin-orders", self?.id, status, page],
    queryFn: () =>
      adminListOrders({
        status: status || undefined,
        page,
        size: 20,
        sort: "createdAt,desc",
      }),
  });
  const rows =
    q.data?.data.filter((o) =>
      `${o.id} ${o.guestName ?? ""} ${o.guestPhone ?? ""}`
        .toLocaleLowerCase("vi")
        .includes(search.toLocaleLowerCase("vi")),
    ) ?? [];
  return (
    <section className="admin-section">
      <header className="admin-section-header">
        <div>
          <h2>Quản lý đơn hàng</h2>
          <p>
            Tra cứu trạng thái, sản phẩm và lịch nhận của các đơn trên hệ thống.
          </p>
        </div>
        <AdminSearch value={search} change={setSearch} />
      </header>
      <AdminFilters
        label="Lọc trạng thái đơn hàng"
        values={{ "": "Tất cả", ...ORDER_STATUS_LABELS }}
        active={status}
        change={(s) => {
          setStatus(s);
          setPage(0);
        }}
      />
      {q.isPending ? (
        <FeatureLoading />
      ) : q.isError ? (
        <FeatureError error={q.error} retry={() => q.refetch()} />
      ) : (
        <>
          <div className="admin-table-wrap">
            <table className="admin-table">
              <caption className="sr-only">Danh sách đơn hàng</caption>
              <thead>
                <tr>
                  <th>Đơn hàng / Người nhận</th>
                  <th>Giá trị</th>
                  <th>Trạng thái</th>
                  <th>Chi tiết</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((o) => (
                  <tr key={o.id}>
                    <td>
                      <strong>#{o.id.slice(0, 8).toUpperCase()}</strong>
                      <span className="admin-cell-detail">
                        {o.guestName ||
                          (o.userId ? "Khách có tài khoản" : "Đơn khách")}{" "}
                        {o.guestPhone || ""}
                      </span>
                      <span className="admin-cell-detail">
                        {o.createdAt ? dateTime(o.createdAt) : ""}
                      </span>
                    </td>
                    <td>
                      <strong>{money(o.totalAmount)}</strong>
                      <span className="admin-cell-detail">
                        {o.paymentStatus === "PAID"
                          ? "Đã thanh toán"
                          : o.paymentStatus === "FAILED"
                            ? "Thanh toán thất bại"
                            : "Chờ thanh toán"}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`admin-badge ${["READY", "COMPLETED"].includes(o.status) ? "positive" : o.status === "CANCELLED" ? "muted" : "warning"}`}
                      >
                        {ORDER_STATUS_LABELS[o.status] || o.status}
                      </span>
                    </td>
                    <td>
                      <details className="admin-order-details">
                        <summary>Xem đơn</summary>
                        <code>{o.id}</code>
                        {o.items?.map((i) => (
                          <p key={i.id}>
                            {i.merchName} × {i.quantity} · {money(i.subtotal)}
                          </p>
                        ))}
                        {o.pickupSchedule && (
                          <p>
                            Nhận {o.pickupSchedule.pickupDate} ·{" "}
                            {o.pickupSchedule.pickupTimeSlot} ·{" "}
                            {o.pickupSchedule.location}
                          </p>
                        )}
                        {o.cancelReason && <p>Lý do hủy: {o.cancelReason}</p>}
                        {o.note && <p>Ghi chú: {o.note}</p>}
                      </details>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!rows.length && (
              <p className="admin-empty">Không có đơn phù hợp.</p>
            )}
          </div>
          <div className="admin-list-footer">
            <span>{q.data?.meta?.totalElements ?? 0} đơn hàng</span>
            <PageControls
              page={page}
              total={q.data?.meta?.totalPages ?? 1}
              change={setPage}
            />
          </div>
        </>
      )}
    </section>
  );
}
export function AdminDashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [activeTab, setActiveTab] = useState("users");
  if (!user) return <Navigate replace state={{ from: "/admin" }} to="/auth" />;
  if (user.role !== "ADMIN") return <Navigate replace to="/" />;
  return (
    <main className="admin-dashboard relative min-h-screen bg-canvas px-4 pb-16 pt-32 sm:px-8">
      <AmbientBackgroundGradients />
      <div className="relative z-10 mx-auto max-w-6xl">
        <header className="mb-7">
          <p className="order-eyebrow">Không gian quản trị</p>
          <h1 className="mt-2 text-3xl font-bold sm:text-4xl">
            Quản trị hệ thống
          </h1>
          <p className="mt-3 text-slate">
            Quản lý tài khoản, tổ chức và đơn hàng trong cùng một nơi.
          </p>
        </header>
        <nav className="admin-main-tabs" aria-label="Khu vực quản trị">
          {[
            { id: "users", label: "Tài khoản", Icon: Users },
            { id: "orgs", label: "Tổ chức", Icon: Building2 },
            { id: "orders", label: "Đơn hàng", Icon: Package },
          ].map(({ id, label, Icon }) => (
            <button
              type="button"
              key={id}
              aria-pressed={activeTab === id}
              onClick={() => setActiveTab(id)}
            >
              <Icon aria-hidden="true" size={18} />
              {label}
            </button>
          ))}
        </nav>
        {activeTab === "users" && <UsersTab />}
        {activeTab === "orgs" && <OrgsTab />}
        {activeTab === "orders" && <AdminOrdersTab />}
      </div>
    </main>
  );
}
