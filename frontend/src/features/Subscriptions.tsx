import { useState } from "react";
import { Bell, Building2, ArrowRight, Check } from "lucide-react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "../stores/toastStore";
import { useAuthStore } from "../stores/authStore";
import {
  listRestock,
  subscribeRestock,
  unsubscribeRestock,
} from "../api/restock";
import {
  listFollowing as listFollowingPage,
  followOrganization,
  updateFollowing as updateFollow,
  unfollowOrganization,
} from "../api/following";
import { collectPages, dateTime } from "../lib/featureUtils";
import { FeatureFrame, FeatureError, FeatureLoading } from "./FeatureUI";
import type { Follow, RestockSubscription } from "../types/features";

export function RestockButton({ merchId }: { merchId: string }) {
  const user = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const key = ["restock", user?.id];
  const q = useQuery({
    queryKey: key,
    queryFn: () => collectPages(listRestock),
    enabled: user?.role === "CUSTOMER",
  });
  const found = q.data?.find((s) => s.merchId === merchId);
  const m = useMutation({
    mutationFn: async () =>
      found ? unsubscribeRestock(merchId) : subscribeRestock(merchId, true),
    onSuccess: () => qc.invalidateQueries({ queryKey: key }),
  });
  if (!user)
    return (
      <Link to="/auth" state={{ from: `/merch/${merchId}` }}>
        Đăng nhập để nhận thông báo có hàng
      </Link>
    );
  if (user.role !== "CUSTOMER") return null;
  return (
    <div className="feature-actions">
      <button
        disabled={q.isPending || q.isError || m.isPending}
        onClick={() => m.mutate()}
      >
        {found ? "Tắt nhắc có hàng" : "Nhắc tôi khi có hàng"}
      </button>
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {m.isError && <FeatureError error={m.error} />}
    </div>
  );
}
export function RestockPage() {
  const user = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const [busy, setBusy] = useState("");
  const key = ["restock", user?.id];
  const q = useQuery({
    queryKey: key,
    queryFn: () => collectPages(listRestock),
    enabled: user?.role === "CUSTOMER",
  });
  const m = useMutation({
    mutationFn: async ({ id, email }: { id: string; email?: boolean }) => {
      setBusy(id);
      return email === undefined
        ? unsubscribeRestock(id)
        : subscribeRestock(id, email);
    },
    onMutate: async ({ id, email }) => {
      await qc.cancelQueries({ queryKey: key });
      const previous = qc.getQueryData<RestockSubscription[]>(key);
      if (email !== undefined)
        qc.setQueryData<RestockSubscription[]>(key, (rows) =>
          rows?.map((row) =>
            row.merchId === id ? { ...row, emailEnabled: email } : row,
          ),
        );
      return { previous };
    },
    onError: (_error, _variables, context) => {
      if (context?.previous && useAuthStore.getState().user?.id === user?.id)
        qc.setQueryData(key, context.previous);
    },
    onSettled: () => {
      setBusy("");
      return qc.invalidateQueries({ queryKey: key });
    },
  });
  return (
    <FeatureFrame
      title="Nhắc khi có hàng"
      description="Quản lý các vật phẩm bạn muốn nhận thông báo khi có hàng trở lại."
      customer
    >
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {m.isError && <FeatureError error={m.error} />}
      {q.data?.length === 0 && (
        <section className="feature-empty-state">
          <Bell aria-hidden="true" />
          <h2>Bạn chưa đăng ký nhắc khi có hàng.</h2>
          <p>
            Khi vật phẩm hết hàng, chọn “Nhắc tôi khi có hàng” để nhận thông báo
            lúc có thể đặt mua.
          </p>
          <Link className="feature-button" to="/merch">
            Khám phá vật phẩm <ArrowRight aria-hidden="true" size={18} />
          </Link>
        </section>
      )}
      {q.data?.map((s) => (
        <section className="feature-panel subscription-card" key={s.merchId}>
          <div className="subscription-identity"><Link to={`/merch/${s.merchId}`}>{s.merchName || "Vật phẩm đang theo dõi"}</Link><p>{s.orgName || "Tổ chức UIT"}</p><span>{s.available ? "Đã có hàng · Xem vật phẩm để đặt mua" : "Đang chờ có hàng trở lại"}</span></div>
          <span>Đăng ký {dateTime(s.subscribedAt)}</span>
          <label>
            <input
              type="checkbox"
              checked={s.emailEnabled}
              disabled={m.isPending || busy === s.merchId}
              onChange={(e) =>
                m.mutate({ id: s.merchId, email: e.target.checked })
              }
            />{" "}
            Nhận qua email
          </label>
          <button
            disabled={m.isPending || busy === s.merchId}
            onClick={() => m.mutate({ id: s.merchId })}
          >
            Huỷ đăng ký
          </button>
        </section>
      ))}
    </FeatureFrame>
  );
}
const defaultFollow = {
  notifyMerch: true,
  notifyEvents: true,
  emailEnabled: false,
};
export function FollowButton({ orgId, onChanged }: { orgId: string; onChanged?: (following: boolean) => void }) {
  const user = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const key = ["following", user?.id];
  const q = useQuery({
    queryKey: key,
    queryFn: () => collectPages(listFollowingPage),
    enabled: user?.role === "CUSTOMER",
  });
  const found = q.data?.some((s) => s.orgId === orgId);
  const m = useMutation({
    mutationFn: async () =>
      found
        ? unfollowOrganization(orgId)
        : followOrganization(orgId, defaultFollow),
    onSuccess: () => {
      if (useAuthStore.getState().user?.id !== user?.id) return;
      onChanged?.(!found);
      toast.success(found ? "Đã bỏ theo dõi tổ chức." : "Đã theo dõi. Bạn sẽ nhận cập nhật vật phẩm và sự kiện mới.");
      return qc.invalidateQueries({ queryKey: key });
    },
  });
  if (!user)
    return (
      <Link to="/auth" state={{ from: `/organization/${orgId}` }}>
        Đăng nhập để theo dõi
      </Link>
    );
  if (user.role !== "CUSTOMER") return null;
  return (
    <div className="follow-control">
      <button aria-pressed={!!found} title={found ? "Bấm để bỏ theo dõi tổ chức" : "Nhận cập nhật của tổ chức"}
        disabled={q.isPending || q.isError || m.isPending}
        onClick={() => m.mutate()}
      >
        {found && <Check aria-hidden="true" size={18} />} {m.isPending ? "Đang cập nhật…" : found ? "Đang theo dõi" : "Theo dõi tổ chức"}
      </button>
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {m.isError && <FeatureError error={m.error} />}
    </div>
  );
}
export function FollowingPage() {
  const user = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const key = ["following", user?.id];
  const q = useQuery({
    queryKey: key,
    queryFn: () => collectPages(listFollowingPage),
    enabled: user?.role === "CUSTOMER",
  });
  const m = useMutation({
    mutationFn: async ({
      id,
      changes,
    }: {
      id: string;
      changes?: Partial<
        Pick<Follow, "notifyMerch" | "notifyEvents" | "emailEnabled">
      >;
    }) => (changes ? updateFollow(id, changes) : unfollowOrganization(id)),
    onMutate: async ({ id, changes }) => {
      await qc.cancelQueries({ queryKey: key });
      const previous = qc.getQueryData<Follow[]>(key);
      if (changes)
        qc.setQueryData<Follow[]>(key, (rows) =>
          rows?.map((row) => (row.orgId === id ? { ...row, ...changes } : row)),
        );
      return { previous };
    },
    onError: (_error, _variables, context) => {
      if (context?.previous && useAuthStore.getState().user?.id === user?.id)
        qc.setQueryData(key, context.previous);
    },
    onSettled: () => qc.invalidateQueries({ queryKey: key }),
  });
  return (
    <FeatureFrame
      title="Tổ chức bạn theo dõi"
      description="Chọn những cập nhật bạn muốn nhận từ các khoa và câu lạc bộ yêu thích."
      customer
    >
      <nav className="subscription-navigation" aria-label="Quản lý theo dõi">
        <Link to="/restock-subscriptions">Nhắc khi có hàng</Link>
        <Link to="/reservations">Đơn giữ chỗ</Link>
      </nav>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {m.isError && <FeatureError error={m.error} />}
      {q.data?.length === 0 && (
        <section className="feature-empty-state">
          <Building2 aria-hidden="true" />
          <h2>Bạn chưa theo dõi tổ chức nào.</h2>
          <p>
            Theo dõi một tổ chức để nhận thông báo khi họ công bố vật phẩm hoặc sự kiện mới. Bạn có thể chọn loại cập nhật trên trang này.
          </p>
          <Link className="feature-button" to="/organization">
            Khám phá tổ chức <ArrowRight aria-hidden="true" size={18} />
          </Link>
        </section>
      )}
      {q.data?.map((s) => (
        <section className="feature-panel subscription-card" key={s.orgId}>
          <div className="subscription-identity organization-follow-identity">
            {s.logoUrl ? <img src={s.logoUrl} alt="" /> : <Building2 aria-hidden="true" size={40} />}
            <div><Link to={`/organization/${s.orgId}`}>{s.orgName || "Tổ chức đang theo dõi"}</Link><p>{s.orgStatus && s.orgStatus !== "ACTIVE" ? "Tổ chức hiện chưa hoạt động" : "Đang theo dõi"}</p></div>
          </div>
          <fieldset className="subscription-preferences"><legend>Nhận cập nhật về</legend>
          {(
            [
              ["notifyMerch", "Vật phẩm mới"],
              ["notifyEvents", "Sự kiện mới"],
              ["emailEnabled", "Gửi thêm qua email"],
            ] as const
          ).map(([field, label]) => (
            <label key={field}>
              <input
                type="checkbox"
                checked={s[field]}
                disabled={m.isPending}
                onChange={(e) =>
                  m.mutate({
                    id: s.orgId,
                    changes: { [field]: e.target.checked },
                  })
                }
              />{" "}
              {label}
            </label>
          ))}
          </fieldset>
          <button className="subscription-unfollow"
            disabled={m.isPending}
            onClick={() => m.mutate({ id: s.orgId })}
          >
            Bỏ theo dõi
          </button>
        </section>
      ))}
    </FeatureFrame>
  );
}
