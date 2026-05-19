import { lazy, Suspense, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import {
  adminListOrders,
  adminListOrganizations,
  adminListUsers,
  adminSetUserActive,
  adminUpdateOrgStatus,
  adminUpdateUserRole,
} from "../api/admin";
import { getApiErrorMessage } from "../api/auth";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { OrderResponse, OrganizationResponse, UserSummaryResponse } from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency",
});

function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("vi-VN");
}

type AdminTab = "users" | "orgs" | "orders";

const ROLE_LABELS: Record<string, string> = {
  CUSTOMER: "Khách hàng",
  ORGANIZER: "Ban tổ chức",
  ADMIN: "Quản trị viên",
};

const ORG_STATUS_OPTIONS = ["PENDING", "ACTIVE", "INACTIVE", "SUSPENDED"];
const ORG_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ duyệt",
  ACTIVE: "Hoạt động",
  INACTIVE: "Tạm dừng",
  SUSPENDED: "Bị khoá",
};

const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  READY_FOR_PICKUP: "Sẵn sàng",
  SUCCESS: "Hoàn thành",
  CANCELLED: "Đã huỷ",
};

// ─── Users Tab ─────────────────────────────────────────────────────────────────

function UsersTab() {
  const [users, setUsers] = useState<UserSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [roleFilter, setRoleFilter] = useState("");
  const [actionId, setActionId] = useState<string | null>(null);

  const load = () => {
    setIsLoading(true);
    adminListUsers({ role: roleFilter || undefined, size: 50 })
      .then(res => setUsers(res.data ?? []))
      .catch(() => toast.error("Không thể tải danh sách người dùng."))
      .finally(() => setIsLoading(false));
  };

  useEffect(() => { load(); }, [roleFilter]);

  const handleRoleChange = async (userId: string, role: string) => {
    setActionId(userId);
    try {
      const res = await adminUpdateUserRole(userId, role);
      setUsers(prev => prev.map(u => u.id === userId ? res.data : u));
      toast.success("Đã cập nhật vai trò.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setActionId(null);
    }
  };

  const handleToggleActive = async (userId: string, current: boolean) => {
    setActionId(userId);
    try {
      const res = await adminSetUserActive(userId, !current);
      setUsers(prev => prev.map(u => u.id === userId ? res.data : u));
      toast.success(current ? "Đã vô hiệu hoá tài khoản." : "Đã kích hoạt tài khoản.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setActionId(null);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-fredoka text-2xl font-bold text-black-blue">Người dùng</h2>
        <div className="flex gap-2">
          {["", "CUSTOMER", "ORGANIZER", "ADMIN"].map(role => (
            <button
              className={[
                "rounded-full border px-4 py-1.5 text-xs font-semibold transition",
                roleFilter === role
                  ? "border-black-blue bg-black-blue text-white"
                  : "border-white/60 bg-white/50 text-black-blue hover:border-black-blue",
              ].join(" ")}
              key={role}
              onClick={() => setRoleFilter(role)}
              type="button"
            >
              {role ? ROLE_LABELS[role] : "Tất cả"}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="py-12 text-center text-sm text-ink/60">Đang tải...</div>
      ) : users.length === 0 ? (
        <div className="py-12 text-center rounded-panel border border-white/40 bg-white/30">
          <p className="text-sm text-ink/60">Không có người dùng nào.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-panel border border-white/50 bg-white/35 shadow-glass backdrop-blur-xl">
          <table className="w-full min-w-[640px] text-sm">
            <thead>
              <tr className="border-b border-white/40 bg-white/20">
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Họ tên</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Email</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Vai trò</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Trạng thái</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Ngày tạo</th>
                <th className="px-4 py-3 text-right text-xs font-semibold uppercase text-ink/50">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/30">
              {users.map(user => (
                <tr className="hover:bg-white/20" key={user.id}>
                  <td className="px-4 py-3 font-semibold text-black-blue">{user.fullName}</td>
                  <td className="px-4 py-3 text-ink/70">{user.email}</td>
                  <td className="px-4 py-3">
                    <select
                      className="rounded-lg border border-white/60 bg-white/50 px-2 py-1 text-xs font-semibold text-black-blue focus:outline-aqua disabled:opacity-50"
                      disabled={actionId === user.id}
                      onChange={e => handleRoleChange(user.id, e.target.value)}
                      value={user.role}
                    >
                      <option value="CUSTOMER">Khách hàng</option>
                      <option value="ORGANIZER">Ban tổ chức</option>
                      <option value="ADMIN">Quản trị viên</option>
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <span className={[
                        "rounded-full px-2 py-0.5 text-[10px] font-semibold border",
                        user.isActive
                          ? "bg-aqua/20 border-aqua/40 text-black-blue"
                          : "bg-peach/20 border-peach/40 text-black-blue",
                      ].join(" ")}>
                        {user.isActive ? "Hoạt động" : "Vô hiệu"}
                      </span>
                      {!user.isVerified && (
                        <span className="rounded-full bg-gold/20 border border-gold/40 px-2 py-0.5 text-[10px] font-semibold text-black-blue">
                          Chưa xác minh
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-xs text-ink/55">{formatDate(user.createdAt)}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      className={[
                        "rounded-full border px-3 py-1 text-xs font-semibold transition disabled:opacity-50",
                        user.isActive
                          ? "border-peach/50 bg-peach/10 text-black-blue hover:bg-peach/30"
                          : "border-aqua/50 bg-aqua/10 text-black-blue hover:bg-aqua/30",
                      ].join(" ")}
                      disabled={actionId === user.id}
                      onClick={() => handleToggleActive(user.id, user.isActive)}
                      type="button"
                    >
                      {user.isActive ? "Vô hiệu hoá" : "Kích hoạt"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ─── Orgs Tab ──────────────────────────────────────────────────────────────────

function OrgsTab() {
  const [orgs, setOrgs] = useState<OrganizationResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("");
  const [actionId, setActionId] = useState<string | null>(null);

  const load = () => {
    setIsLoading(true);
    adminListOrganizations({ status: statusFilter || undefined, size: 50 })
      .then(res => setOrgs(res.data ?? []))
      .catch(() => toast.error("Không thể tải danh sách tổ chức."))
      .finally(() => setIsLoading(false));
  };

  useEffect(() => { load(); }, [statusFilter]);

  const handleStatusChange = async (orgId: string, status: string) => {
    setActionId(orgId);
    try {
      const res = await adminUpdateOrgStatus(orgId, status);
      setOrgs(prev => prev.map(o => o.id === orgId ? res.data : o));
      toast.success("Đã cập nhật trạng thái tổ chức.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setActionId(null);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-fredoka text-2xl font-bold text-black-blue">Tổ chức</h2>
        <div className="flex flex-wrap gap-2">
          {["", ...ORG_STATUS_OPTIONS].map(s => (
            <button
              className={[
                "rounded-full border px-4 py-1.5 text-xs font-semibold transition",
                statusFilter === s
                  ? "border-black-blue bg-black-blue text-white"
                  : "border-white/60 bg-white/50 text-black-blue hover:border-black-blue",
              ].join(" ")}
              key={s}
              onClick={() => setStatusFilter(s)}
              type="button"
            >
              {s ? ORG_STATUS_LABELS[s] ?? s : "Tất cả"}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="py-12 text-center text-sm text-ink/60">Đang tải...</div>
      ) : orgs.length === 0 ? (
        <div className="py-12 text-center rounded-panel border border-white/40 bg-white/30">
          <p className="text-sm text-ink/60">Không có tổ chức nào.</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {orgs.map(org => (
            <div className="rounded-[28px] border border-white/50 bg-white/40 p-5 shadow-glass backdrop-blur-xl" key={org.id}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="font-fredoka text-lg font-bold text-black-blue">{org.name}</h3>
                  <p className="text-xs text-ink/55 mt-0.5">{formatDate(org.createdAt)}</p>
                </div>
                <span className={[
                  "rounded-full border px-2.5 py-0.5 text-xs font-semibold shrink-0",
                  org.status === "ACTIVE"
                    ? "bg-aqua/20 border-aqua/40 text-black-blue"
                    : org.status === "PENDING"
                      ? "bg-gold/20 border-gold/40 text-black-blue"
                      : "bg-peach/20 border-peach/40 text-black-blue",
                ].join(" ")}>
                  {ORG_STATUS_LABELS[org.status] ?? org.status}
                </span>
              </div>
              {org.description && (
                <p className="mt-2 line-clamp-2 text-xs text-ink/60">{org.description}</p>
              )}
              <div className="mt-3 flex flex-wrap gap-2 border-t border-white/30 pt-3">
                {ORG_STATUS_OPTIONS.filter(s => s !== org.status).map(s => (
                  <button
                    className="rounded-full border border-aqua/50 bg-aqua/10 px-3 py-1 text-xs font-semibold text-black-blue hover:bg-aqua/30 disabled:opacity-50"
                    disabled={actionId === org.id}
                    key={s}
                    onClick={() => handleStatusChange(org.id, s)}
                    type="button"
                  >
                    → {ORG_STATUS_LABELS[s]}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Admin Orders Tab ──────────────────────────────────────────────────────────

function AdminOrdersTab() {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("");

  const load = () => {
    setIsLoading(true);
    adminListOrders({ status: statusFilter || undefined, size: 50 })
      .then(res => setOrders(res.data ?? []))
      .catch(() => toast.error("Không thể tải đơn hàng."))
      .finally(() => setIsLoading(false));
  };

  useEffect(() => { load(); }, [statusFilter]);

  const ADMIN_ORDER_TABS = [
    { label: "Tất cả", value: "" },
    { label: "Chờ xác nhận", value: "PENDING" },
    { label: "Đã xác nhận", value: "CONFIRMED" },
    { label: "Sẵn sàng", value: "READY_FOR_PICKUP" },
    { label: "Hoàn thành", value: "SUCCESS" },
    { label: "Đã huỷ", value: "CANCELLED" },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-fredoka text-2xl font-bold text-black-blue">Tất cả đơn hàng</h2>
      </div>

      <div className="flex flex-wrap gap-2">
        {ADMIN_ORDER_TABS.map(tab => (
          <button
            className={[
              "rounded-full border px-4 py-1.5 text-xs font-semibold transition",
              statusFilter === tab.value
                ? "border-black-blue bg-black-blue text-white"
                : "border-white/60 bg-white/50 text-black-blue hover:border-black-blue",
            ].join(" ")}
            key={tab.value}
            onClick={() => setStatusFilter(tab.value)}
            type="button"
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="py-12 text-center text-sm text-ink/60">Đang tải...</div>
      ) : orders.length === 0 ? (
        <div className="py-12 text-center rounded-panel border border-white/40 bg-white/30">
          <p className="text-sm text-ink/60">Không có đơn hàng nào.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-panel border border-white/50 bg-white/35 shadow-glass backdrop-blur-xl">
          <table className="w-full min-w-[640px] text-sm">
            <thead>
              <tr className="border-b border-white/40 bg-white/20">
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Đơn #</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Khách</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Tổng tiền</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Trạng thái</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Ngày tạo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/30">
              {orders.map(order => (
                <tr className="hover:bg-white/20" key={order.id}>
                  <td className="px-4 py-3 font-mono text-xs font-semibold text-black-blue">
                    {order.id.slice(0, 8).toUpperCase()}
                  </td>
                  <td className="px-4 py-3 text-ink/70">
                    {order.guestName || "—"}
                    {order.guestPhone && <span className="block text-xs text-ink/40">{order.guestPhone}</span>}
                  </td>
                  <td className="px-4 py-3 font-semibold text-black-blue">
                    {currencyFormatter.format(order.totalAmount)}
                  </td>
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-white/50 border border-white/60 px-2.5 py-0.5 text-xs font-semibold text-black-blue">
                      {ORDER_STATUS_LABELS[order.status] ?? order.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-ink/55">{formatDate(order.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ─── Main Admin Dashboard ──────────────────────────────────────────────────────

export function AdminDashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [activeTab, setActiveTab] = useState<AdminTab>("users");

  if (!user) {
    return <Navigate replace state={{ from: "/admin" }} to="/auth" />;
  }

  if (user.role !== "ADMIN") {
    return <Navigate replace to="/" />;
  }

  const TABS: { id: AdminTab; label: string }[] = [
    { id: "users", label: "Người dùng" },
    { id: "orgs", label: "Tổ chức" },
    { id: "orders", label: "Đơn hàng" },
  ];

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <div className="relative z-10 mx-auto max-w-6xl">
        <div className="mb-6">
          <p className="font-sans text-xs uppercase tracking-widest text-ink/50">
            Admin
          </p>
          <h1 className="mt-1 font-fredoka text-4xl font-bold text-black-blue">
            Quản trị hệ thống
          </h1>
        </div>

        <div className="mb-6 flex gap-1 rounded-full border border-white/50 bg-white/40 p-1 w-fit shadow-glass backdrop-blur-xl">
          {TABS.map(tab => (
            <button
              className={[
                "rounded-full px-5 py-2 text-sm font-semibold transition",
                activeTab === tab.id
                  ? "bg-black-blue text-white shadow"
                  : "text-black-blue hover:bg-white/50",
              ].join(" ")}
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              type="button"
            >
              {tab.label}
            </button>
          ))}
        </div>

        {activeTab === "users" && <UsersTab />}
        {activeTab === "orgs" && <OrgsTab />}
        {activeTab === "orders" && <AdminOrdersTab />}
      </div>
    </main>
  );
}
