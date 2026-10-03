import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowRight,
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  Layers3,
  Target,
  ShoppingBag,
  Info,
} from "lucide-react";
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
function campaignDeadline(value: string) {
  const date = new Date(
    /[Zz]|[+-]\d\d:\d\d$/.test(value) ? value : value + "+07:00",
  );
  if (!Number.isFinite(date.getTime())) return "Đang cập nhật";
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
function CampaignCard({
  campaign,
  own,
  hideTitle = false,
}: {
  campaign: Campaign;
  own?: boolean;
  hideTitle?: boolean;
}) {
  const reached = campaign.reservedQuantity >= campaign.minimumQuantity;
  const expired =
    campaign.state === "ACTIVE" && Date.parse(campaign.deadline) <= Date.now();
  const stateLabel = expired ? "Đã hết hạn đặt trước" : labels[campaign.state];
  return (
    <article
      className="feature-panel campaign-card"
      aria-label={hideTitle ? "Tiến độ đặt trước" : campaign.title}
    >
      <header className="campaign-card-heading">
        <span className="campaign-eyebrow">
          <Layers3 aria-hidden="true" size={16} /> Bộ sưu tập UIT
        </span>
        <span
          className={`campaign-state campaign-state-${campaign.state.toLowerCase()}`}
        >
          <span aria-hidden="true" />
          {stateLabel}
        </span>
      </header>
      {!hideTitle && <h2>{campaign.title}</h2>}
      {!hideTitle && campaign.description && (
        <p className="campaign-card-description">{campaign.description}</p>
      )}
      <div className="campaign-metrics">
        <section className="campaign-goal" aria-label="Mục tiêu đặt trước">
          <p>
            <Target aria-hidden="true" size={18} /> Mục tiêu tối thiểu
          </p>
          <strong>
            {campaign.minimumQuantity.toLocaleString("vi-VN")}{" "}
            <span>sản phẩm</span>
          </strong>
        </section>
        <section className="campaign-deadline" aria-label="Hạn đặt trước">
          <p>
            <CalendarDays aria-hidden="true" size={18} /> Hạn đặt trước
          </p>
          <time dateTime={campaign.deadline}>
            {campaignDeadline(campaign.deadline)}
          </time>
          <span>Giờ Việt Nam</span>
        </section>
      </div>
      <div className="campaign-progress-heading">
        <span>Đã giữ chỗ</span>
        <strong>
          {campaign.reservedQuantity.toLocaleString("vi-VN")} /{" "}
          {campaign.minimumQuantity.toLocaleString("vi-VN")} sản phẩm
        </strong>
      </div>
      <progress
        aria-label="Tiến độ đặt trước"
        max={campaign.minimumQuantity}
        value={Math.min(campaign.reservedQuantity, campaign.minimumQuantity)}
      />
      <p className="campaign-progress-note">
        {campaign.state === "SUCCEEDED"
          ? "Đã đạt mục tiêu · Ban tổ chức sẽ thông báo lịch nhận."
          : campaign.state !== "ACTIVE" || expired
            ? "Đợt đặt trước đã đóng. Xem chi tiết để theo dõi thông tin."
            : reached
              ? "Đã đủ số lượng mục tiêu. Theo dõi cập nhật từ ban tổ chức."
              : `Còn ${Math.max(0, campaign.minimumQuantity - campaign.reservedQuantity)} sản phẩm để đạt mục tiêu.`}
      </p>
      {!own && (
        <Link className="campaign-discover" to={`/campaigns/${campaign.id}`}>
          Khám phá bộ sưu tập <ArrowRight aria-hidden="true" size={18} />
        </Link>
      )}
    </article>
  );
}
export function CampaignsPage() {
  const [page, setPage] = useState(0);
  const q = useQuery({
    queryKey: ["campaigns", "public", page],
    queryFn: () => listCampaigns(page),
  });
  return (
    <FeatureFrame
      title="Bộ sưu tập mở đặt trước"
      description="Giữ chỗ cho mẫu bạn yêu thích. Đợt đặt trước được triển khai khi đạt đủ số lượng đăng ký."
    >
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data?.content.length === 0 && <p>Chưa có bộ sưu tập mở đặt trước.</p>}
      <div className="campaign-grid">
        {q.data?.content.map((c) => (
          <CampaignCard key={c.id} campaign={c} />
        ))}
      </div>
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
    mutationFn: async ({
      userId,
      campaignId,
    }: {
      userId: string;
      campaignId: string;
    }) => {
      if (useAuthStore.getState().user?.id !== userId)
        throw new Error("Tài khoản đã thay đổi. Vui lòng thao tác lại.");
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
    onSuccess: (_result, variables) => {
      if (
        useAuthStore.getState().user?.id !== variables.userId ||
        id !== variables.campaignId
      )
        return;
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
    <FeatureFrame
      title={q.data?.title ?? "Đợt mở đặt trước"}
      description="Giữ chỗ hôm nay, nhận vật phẩm khi bộ sưu tập được triển khai."
    >
      <Link className="campaign-back-link" to="/campaigns">
        <ArrowLeft aria-hidden="true" size={17} /> Bộ sưu tập mở đặt trước
      </Link>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data && (
        <>
          <CampaignCard campaign={q.data} own hideTitle />
          <div className="campaign-detail-columns">
            <section className="feature-panel campaign-story">
              <h2>Về bộ sưu tập</h2>
              <p>
                {q.data.description ||
                  "Chọn phiên bản bạn yêu thích và giữ chỗ trong thời gian mở đặt trước."}
              </p>
              <h3>Đặt trước hoạt động thế nào?</h3>
              <ol className="campaign-steps">
                <li>
                  <span>1</span>
                  <div>
                    <strong>Chọn phiên bản và giữ chỗ</strong>
                    <p>
                      Bạn đăng ký số lượng muốn nhận. Bước này chưa thu tiền.
                    </p>
                  </div>
                </li>
                <li>
                  <span>2</span>
                  <div>
                    <strong>Cùng đạt mục tiêu</strong>
                    <p>
                      Đơn được xác nhận khi đợt đặt trước đạt đủ số lượng tối
                      thiểu.
                    </p>
                  </div>
                </li>
                <li>
                  <span>3</span>
                  <div>
                    <strong>Nhận thông báo và nhận hàng</strong>
                    <p>
                      Ban tổ chức xác nhận đơn và thông báo lịch nhận cho bạn.
                    </p>
                  </div>
                </li>
              </ol>
            </section>
            <section
              className="feature-panel campaign-reserve"
              aria-label="Thông tin giữ chỗ"
            >
              <header className="campaign-reserve-heading">
                <ShoppingBag aria-hidden="true" size={22} />
                <div>
                  <h2>
                    {active || intent
                      ? "Giữ chỗ của bạn"
                      : "Thông tin đợt đặt trước"}
                  </h2>
                  <p>Đơn giữ chỗ chưa phải thanh toán.</p>
                </div>
              </header>
              {!active && !intent ? (
                <div className="campaign-closed" role="status">
                  <CheckCircle2 aria-hidden="true" size={26} />
                  <h3>
                    {q.data.state === "SUCCEEDED"
                      ? "Bộ sưu tập đã đạt mục tiêu"
                      : "Đợt đặt trước đã kết thúc"}
                  </h3>
                  <p>
                    {q.data.state === "SUCCEEDED"
                      ? "Đã ngừng nhận giữ chỗ mới. Nếu bạn đã đặt trước, hãy theo dõi xác nhận và lịch nhận trong đơn hàng."
                      : "Đợt này không còn nhận giữ chỗ mới. Bạn có thể khám phá các bộ sưu tập khác."}
                  </p>
                  <Link
                    className="feature-button"
                    to={
                      user?.role === "CUSTOMER" ? "/reservations" : "/campaigns"
                    }
                  >
                    {user?.role === "CUSTOMER"
                      ? "Xem đơn giữ chỗ"
                      : "Khám phá bộ sưu tập"}
                    <ArrowRight aria-hidden="true" size={18} />
                  </Link>
                </div>
              ) : !user ? (
                <div className="campaign-sign-in">
                  <p>
                    Đăng nhập bằng tài khoản khách hàng để lưu đơn giữ chỗ và
                    nhận cập nhật.
                  </p>
                  <Link
                    className="feature-button"
                    to="/auth"
                    state={{ from: `/campaigns/${id}` }}
                  >
                    Đăng nhập để đặt trước
                  </Link>
                </div>
              ) : user.role !== "CUSTOMER" ? (
                <p className="campaign-role-note">
                  Chỉ tài khoản khách hàng có thể đặt trước.
                </p>
              ) : (
                <>
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
                        rows={3}
                        disabled={!!intent || m.isPending || !active}
                        onChange={(e) => setNote(e.target.value)}
                        placeholder="Ví dụ: thời gian nhận hàng mong muốn…"
                      />
                    </label>
                  </div>
                  {selected && Number.isInteger(quantity) && quantity > 0 && (
                    <div className="campaign-cost">
                      <span>Giá trị dự kiến</span>
                      <strong>{money(selected.unitPrice * quantity)}</strong>
                      <p>
                        {selected.label} × {quantity}. Chưa thu tiền tại bước
                        giữ chỗ.
                      </p>
                    </div>
                  )}
                  {intent && (
                    <p className="campaign-pending" role="status">
                      <Info aria-hidden="true" size={18} /> Yêu cầu đang chờ xác
                      nhận. Thử lại để kiểm tra kết quả; hệ thống giữ nguyên yêu
                      cầu để tránh tạo đơn trùng.
                    </p>
                  )}
                  <button
                    className="campaign-submit"
                    type="button"
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
                    onClick={() =>
                      m.mutate({ userId: user.id, campaignId: id })
                    }
                  >
                    {m.isPending
                      ? "Đang gửi yêu cầu…"
                      : intent
                        ? "Thử lại yêu cầu đang chờ"
                        : "Giữ chỗ"}
                    <ArrowRight aria-hidden="true" size={18} />
                  </button>
                  {!selected && !intent && (
                    <p className="campaign-form-hint">
                      Chọn phiên bản để xem giá và giữ chỗ.
                    </p>
                  )}
                  {!!storageError && <FeatureError error={storageError} />}
                  {m.isError && <FeatureError error={m.error} />}
                  {m.isSuccess &&
                    m.variables.userId === user.id &&
                    m.variables.campaignId === id && (
                      <p className="campaign-success" role="status">
                        <CheckCircle2 aria-hidden="true" size={18} /> Đã giữ
                        chỗ.{" "}
                        <Link to={`/orders/${m.data.order.id}`}>
                          Xem đơn hàng
                        </Link>
                      </p>
                    )}
                </>
              )}
            </section>
          </div>
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
