import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  listCampaigns,
  getCampaign,
  reserveCampaign,
  listReservations,
  createCampaign,
  cancelCampaign,
  getCampaignOrderContext,
} from "../api/campaigns";
import { getOwnMerch } from "../api/merch";
import { useAuthStore } from "../stores/authStore";
import {
  dateTime,
  money,
  normalizePage,
  collectPages,
} from "../lib/featureUtils";
import { invalidateFeatures } from "../lib/queryClient";
import {
  FeatureFrame,
  FeatureError,
  FeatureLoading,
  PageControls,
} from "./FeatureUI";
import type { Campaign, ReserveRequest } from "../types/features";

const labels = {
  ACTIVE: "Đang nhận đặt trước",
  SUCCEEDED: "Đạt mục tiêu",
  FAILED: "Không đạt mục tiêu",
  CANCELLED: "Đã huỷ",
};
function CampaignCard({
  campaign,
  own,
}: {
  campaign: Campaign;
  own?: boolean;
}) {
  return (
    <section className="feature-panel campaign-card">
      <p className="order-eyebrow">Bộ sưu tập UIT</p>
      <h2>{campaign.title}</h2>
      <p>
        {labels[campaign.state]} · {campaign.reservedQuantity}/
        {campaign.minimumQuantity} sản phẩm
      </p>
      <progress aria-label="Tiến độ đặt trước" max={campaign.minimumQuantity} value={Math.min(campaign.reservedQuantity, campaign.minimumQuantity)} />
      <p>Hạn đặt: {dateTime(campaign.deadline)}</p>
      {!own && <Link to={`/campaigns/${campaign.id}`}>Khám phá bộ sưu tập</Link>}
    </section>
  );
}
export function CampaignsPage() {
  const [page, setPage] = useState(0);
  const q = useQuery({
    queryKey: ["campaigns", "public", page],
    queryFn: () => listCampaigns(page),
  });
  return (
    <FeatureFrame title="Bộ sưu tập mở đặt trước" description="Giữ chỗ cho mẫu bạn yêu thích. Đợt đặt trước được triển khai khi đạt đủ số lượng đăng ký.">
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data?.content.length === 0 && <p>Chưa có bộ sưu tập mở đặt trước.</p>}
      {q.data?.content.map((c) => (
        <CampaignCard key={c.id} campaign={c} />
      ))}
      {q.data && (
        <PageControls page={page} total={q.data.totalPages} change={setPage} />
      )}
    </FeatureFrame>
  );
}
export function CampaignDetailPage() {
  const { id = "" } = useParams();
  const user = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const key = `uitmerch-reservation:${user?.id}:${id}`;
  const [variant, setVariant] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [note, setNote] = useState("");
  const [intent, setIntent] = useState<ReserveRequest>();
  const [storageError, setStorageError] = useState<unknown>();
  useEffect(() => {
    m.reset();
    setIntent(undefined);
    setStorageError(undefined);
    setVariant("");
    setQuantity(1);
    setNote("");
    try {
      const saved = sessionStorage.getItem(key);
      if (saved) {
        const value = JSON.parse(saved) as ReserveRequest;
        setIntent(value);
        setVariant(value.merchId);
        setQuantity(value.quantity);
        setNote(value.note ?? "");
      }
    } catch (e) {
      setStorageError(e);
    }
  }, [key]);
  const q = useQuery({
    queryKey: ["campaign", id],
    queryFn: () => getCampaign(id),
    refetchInterval: (query) =>
      query.state.data?.state === "ACTIVE" ? 30000 : false,
  });
  const m = useMutation({
    mutationFn: async ({userId,campaignId}: {userId:string;campaignId:string}) => {
      if(useAuthStore.getState().user?.id !== userId) throw new Error("Tài khoản đã thay đổi. Vui lòng thao tác lại.");
      let request = intent;
      if (!request) {
        request = {
          merchId: variant,
          quantity,
          requestId: crypto.randomUUID(),
          note: note.trim() || undefined,
        };
        sessionStorage.setItem(key, JSON.stringify(request));
        setIntent(request);
      }
      return reserveCampaign(campaignId, request);
    },
    onSuccess: (_result,variables) => {
      if(useAuthStore.getState().user?.id !== variables.userId || id !== variables.campaignId) return;
      sessionStorage.removeItem(key);
      setIntent(undefined);
      invalidateFeatures();
      void qc.invalidateQueries({ queryKey: ["campaign", id] });
    },
  });
  const active =
    q.data?.state === "ACTIVE" && Date.parse(q.data.deadline) > Date.now();
  const selected = q.data?.variants.find((v) => v.merchId === variant);
  return (
    <FeatureFrame title={q.data?.title ?? "Đợt mở đặt trước"}>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data && (
        <>
          <CampaignCard campaign={q.data} own />
          <p>{q.data.description}</p>
          <p>
            Đơn giữ chỗ chỉ được xác nhận khi chiến dịch đạt mục tiêu. Đây chưa
            phải thanh toán.
          </p>
          {!user ? (
            <Link to="/auth" state={{ from: `/campaigns/${id}` }}>
              Đăng nhập để đặt trước
            </Link>
          ) : user.role !== "CUSTOMER" ? (
            <p>Chỉ tài khoản khách hàng có thể đặt trước.</p>
          ) : (
            <section className="feature-panel">
              <div className="feature-fields">
                <label>
                  Phiên bản
                  <select
                    value={variant}
                    disabled={!!intent || m.isPending || !active}
                    onChange={(e) => setVariant(e.target.value)}
                  >
                    <option value="">Chọn phiên bản</option>
                    {q.data.variants.map((v) => (
                      <option
                        key={v.merchId}
                        value={v.merchId}
                        disabled={!v.available || v.availableQuantity < 1}
                      >
                        {v.label} · {money(v.unitPrice)} · Còn{" "}
                        {v.availableQuantity}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Số lượng
                  <input
                    type="number"
                    min={1}
                    max={Math.min(100, selected?.availableQuantity ?? 100)}
                    value={quantity}
                    disabled={!!intent || m.isPending || !active}
                    onChange={(e) => setQuantity(Number(e.target.value))}
                  />
                </label>
                <label>
                  Ghi chú
                  <textarea
                    value={note}
                    maxLength={1000}
                    disabled={!!intent || m.isPending || !active}
                    onChange={(e) => setNote(e.target.value)}
                  />
                </label>
              </div>
              {intent && (
                <p>
                  Yêu cầu đang chờ xác nhận. Thử lại giữ nguyên mã yêu cầu và
                  nội dung, tránh tạo đơn trùng.
                </p>
              )}
              <div className="feature-actions">
                <button
                  disabled={
                    m.isPending ||
                    !!storageError ||
                    (!intent &&
                      (!active ||
                        !selected?.available ||
                        !Number.isInteger(quantity) ||
                        quantity < 1 ||
                        quantity > Math.min(100, selected.availableQuantity)))
                  }
                  onClick={() => m.mutate({userId:user.id,campaignId:id})}
                >
                  {intent ? "Thử lại yêu cầu đang chờ" : "Giữ chỗ"}
                </button>
              </div>
              {!!storageError && <FeatureError error={storageError} />}
              {m.isError && <FeatureError error={m.error} />}
              {m.isSuccess && m.variables.userId === user.id && m.variables.campaignId === id && (
                <p role="status">
                  Đã giữ chỗ.{" "}
                  <Link to={`/orders/${m.data.order.id}`}>Xem đơn hàng</Link>
                </p>
              )}
            </section>
          )}
        </>
      )}
    </FeatureFrame>
  );
}
export function ReservationsPage() {
  const user = useAuthStore((s) => s.user);
  const [page, setPage] = useState(0);
  const q = useQuery({
    queryKey: ["reservations", user?.id, page],
    queryFn: () => listReservations(page),
    enabled: user?.role === "CUSTOMER",
  });
  return (
    <FeatureFrame title="Đơn giữ chỗ" customer>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data?.content.length === 0 && <p>Chưa có đơn giữ chỗ.</p>}
      {q.data?.content.map((r) => (
        <section className="feature-panel feature-actions" key={r.id}>
          <p>
            Số lượng {r.quantity} · {dateTime(r.createdAt)}
          </p>
          <Link to={`/campaigns/${r.campaignId}`}>Đợt đặt trước</Link>
          <Link to={`/orders/${r.orderId}`}>Trạng thái / huỷ đơn</Link>
        </section>
      ))}
      {q.data && (
        <PageControls page={page} total={q.data.totalPages} change={setPage} />
      )}
    </FeatureFrame>
  );
}
export function CampaignOrderNotice({
  orderId,
  orgId,
  children,
}: {
  orderId: string;
  orgId?: string;
  children?: (allowed: boolean) => React.ReactNode;
}) {
  const user = useAuthStore((s) => s.user);
  const q = useQuery({
    queryKey: ["campaign-order", user?.id, orgId, orderId],
    queryFn: () => getCampaignOrderContext(orderId, orgId),
    enabled: !!user,
  });
  return (
    <div>
      {q.isPending && <p>Đang kiểm tra điều kiện xử lý đơn…</p>}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data?.campaignId && (
        <p>
          Đợt đặt trước:{" "}
          {q.data.campaignState
            ? labels[q.data.campaignState]
            : "Đang kiểm tra"}
          .{" "}
          {!q.data.fulfillmentAllowed &&
            "Chưa thể xác nhận hoặc xếp lịch nhận hàng."}
        </p>
      )}
      {children?.(q.data?.fulfillmentAllowed === true)}
    </div>
  );
}
export function CampaignManagement({ orgId }: { orgId: string }) {
  const user = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [minimum, setMinimum] = useState(1);
  const [deadline, setDeadline] = useState("");
  const [variants, setVariants] = useState<Record<string, string>>({});
  const key = ["campaigns", user?.id, orgId];
  const q = useQuery({
    queryKey: [...key, page],
    queryFn: () => listCampaigns(page, orgId),
  });
  const merch = useQuery({
    queryKey: ["campaign-merch", user?.id, orgId],
    queryFn: () =>
      collectPages(async (p) => {
        const res = normalizePage(
          await getOwnMerch(orgId, { page: p, size: 100 }),
        );
        return {
          content: res.items,
          number: p,
          size: 100,
          totalElements: res.meta?.totalElements ?? res.items.length,
          totalPages: res.meta?.totalPages ?? 1,
          last: !res.meta?.hasNext,
          first: p === 0,
        };
      }),
  });
  const create = useMutation({
    mutationFn: () =>
      createCampaign(orgId, {
        title: title.trim(),
        description: description.trim() || undefined,
        minimumQuantity: minimum,
        deadline: new Date(deadline + ":00+07:00").toISOString(),
        variants: Object.entries(variants).map(([merchId, label]) => ({
          merchId,
          label,
        })),
      }),
    onSuccess: () => {
      setTitle("");
      setDescription("");
      setVariants({});
      void qc.invalidateQueries({ queryKey: key });
    },
  });
  const cancel = useMutation({
    mutationFn: (id: string) => cancelCampaign(orgId, id),
    onSuccess: () => {
      invalidateFeatures();
    },
  });
  return (
    <>
      <section className="feature-panel">
        <h2>Tạo đợt đặt trước</h2>
        <p>
          Giờ đóng chiến dịch theo múi giờ Việt Nam. Chọn 1–20 SKU đã công bố,
          đặt tên phiên bản rõ ràng.
        </p>
        <div className="feature-fields">
          <label>
            Tên đợt đặt trước
            <input
              maxLength={200}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </label>
          <label>
            Mô tả
            <textarea
              maxLength={4000}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </label>
          <label>
            Số lượng tối thiểu
            <input
              type="number"
              min={1}
              max={100000}
              value={minimum}
              onChange={(e) => setMinimum(Number(e.target.value))}
            />
          </label>
          <label>
            Hạn đóng (giờ Việt Nam)
            <input
              type="datetime-local"
              value={deadline}
              onChange={(e) => setDeadline(e.target.value)}
            />
          </label>
          {merch.data
            ?.filter((m) => m.status === "PUBLISHED")
            .map((m) => (
              <label key={m.id}>
                <span>
                  <input
                    type="checkbox"
                    checked={m.id in variants}
                    disabled={create.isPending}
                    onChange={(e) =>
                      setVariants((v) => {
                        const next = { ...v };
                        if (e.target.checked) next[m.id] = m.name;
                        else delete next[m.id];
                        return next;
                      })
                    }
                  />
                  {m.name} · Còn {m.stock}
                </span>
                {m.id in variants && (
                  <input
                    aria-label={`Tên phiên bản ${m.name}`}
                    maxLength={128}
                    value={variants[m.id]}
                    onChange={(e) =>
                      setVariants((v) => ({ ...v, [m.id]: e.target.value }))
                    }
                  />
                )}
              </label>
            ))}
        </div>
        <div className="feature-actions">
          <button
            disabled={
              create.isPending ||
              !title.trim() ||
              !deadline ||
              !Number.isInteger(minimum) ||
              minimum < 1 ||
              Object.keys(variants).length < 1 ||
              Object.keys(variants).length > 20 ||
              Object.values(variants).some((v) => !v.trim())
            }
            onClick={() => create.mutate()}
          >
            Tạo đợt đặt trước
          </button>
        </div>
        {merch.isPending && <FeatureLoading />}
        {merch.isError && (
          <FeatureError error={merch.error} retry={() => merch.refetch()} />
        )}
        {create.isError && <FeatureError error={create.error} />}
        {create.isSuccess && <p role="status">Đã tạo chiến dịch.</p>}
      </section>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {cancel.isError && <FeatureError error={cancel.error} />}
      {q.data?.content.map((c) => (
        <section key={c.id}>
          <CampaignCard campaign={c} own />
          <details>
            <summary>Phiên bản và giá giữ chỗ</summary>
            <OwnedCampaignDetail id={c.id} orgId={orgId} />
          </details>
          {c.state === "ACTIVE" && (
            <div className="feature-actions">
              <button
                disabled={cancel.isPending}
                onClick={() => {
                  if (window.confirm("Huỷ chiến dịch và các đơn giữ chỗ?"))
                    cancel.mutate(c.id);
                }}
              >
                Huỷ chiến dịch {c.title}
              </button>
            </div>
          )}
        </section>
      ))}
      {q.data && (
        <PageControls page={page} total={q.data.totalPages} change={setPage} />
      )}
    </>
  );
}

function OwnedCampaignDetail({ id, orgId }: { id: string; orgId: string }) {
  const user = useAuthStore((s) => s.user);
  const q = useQuery({
    queryKey: ["campaign-owned", user?.id, orgId, id],
    queryFn: () => getCampaign(id, orgId),
  });
  return (
    <section>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data?.variants.map((v) => (
        <p key={v.merchId}>
          {v.label} · {money(v.unitPrice)} · Còn {v.availableQuantity}
        </p>
      ))}
    </section>
  );
}
