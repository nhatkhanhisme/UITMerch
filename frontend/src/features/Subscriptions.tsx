import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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
        {found ? "Huỷ báo có hàng" : "Báo tôi khi có hàng"}
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
    <FeatureFrame title="Thông báo có hàng" customer>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {m.isError && <FeatureError error={m.error} />}
      {q.data?.length === 0 && <p>Bạn chưa đăng ký báo có hàng.</p>}
      {q.data?.map((s) => (
        <section className="feature-panel feature-actions" key={s.merchId}>
          <Link to={`/merch/${s.merchId}`}>Xem vật phẩm</Link>
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
export function FollowButton({ orgId }: { orgId: string }) {
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
    onSuccess: () => qc.invalidateQueries({ queryKey: key }),
  });
  if (!user)
    return (
      <Link to="/auth" state={{ from: `/organization/${orgId}` }}>
        Đăng nhập để theo dõi
      </Link>
    );
  if (user.role !== "CUSTOMER") return null;
  return (
    <div className="feature-actions">
      <button
        disabled={q.isPending || q.isError || m.isPending}
        onClick={() => m.mutate()}
      >
        {found ? "Bỏ theo dõi" : "Theo dõi tổ chức"}
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
    <FeatureFrame title="Tổ chức đang theo dõi" customer>
      <p>
        <Link to="/restock-subscriptions">Quản lý báo có hàng</Link> ·{" "}
        <Link to="/reservations">Đơn giữ chỗ</Link>
      </p>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {m.isError && <FeatureError error={m.error} />}
      {q.data?.length === 0 && <p>Bạn chưa theo dõi tổ chức nào.</p>}
      {q.data?.map((s) => (
        <section className="feature-panel feature-actions" key={s.orgId}>
          <Link to={`/organization/${s.orgId}`}>Xem tổ chức</Link>
          {(
            [
              ["notifyMerch", "Vật phẩm mới"],
              ["notifyEvents", "Sự kiện mới"],
              ["emailEnabled", "Email"],
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
          <button
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
