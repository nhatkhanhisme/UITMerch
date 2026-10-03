import { useEffect, useRef, useState } from "react";
import { QrCode, Clock3, History, Check } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import type { IScannerControls } from "@zxing/browser";
import {
  issuePickup,
  verifyPickup,
  completePickup,
  requestGuestReceipt,
  exchangeGuestReceipt,
  trackGuestOrder,
  getOrderHistory,
} from "../api/pickup";
import { useAuthStore } from "../stores/authStore";
import { dateTime, featureError } from "../lib/featureUtils";
import { invalidateFeatures } from "../lib/queryClient";
import {
  FeatureFrame,
  FeatureError,
  FeatureLoading,
  PageControls,
} from "./FeatureUI";
import type { OrderResponse } from "../types/shared";
import type { PickupCredential } from "../types/features";

export function PickupQR({ credential }: { credential: PickupCredential }) {
  const ref = useRef<HTMLCanvasElement>(null);
  const [expired, setExpired] = useState(
    Date.parse(credential.expiresAt) <= Date.now(),
  );
  const [error, setError] = useState<unknown>();
  useEffect(() => {
    setError(undefined);
    setExpired(Date.parse(credential.expiresAt) <= Date.now());
    if (ref.current)
      void import("qrcode")
        .then(({ default: QRCode }) => {
          if (ref.current)
            return QRCode.toCanvas(ref.current, credential.token, {
              width: 240,
              margin: 2,
            });
        })
        .catch(setError);
    const timer = setTimeout(
      () => setExpired(true),
      Math.max(0, Date.parse(credential.expiresAt) - Date.now()),
    );
    return () => clearTimeout(timer);
  }, [credential]);
  return (
    <section className="pickup-credential" aria-label="Mã nhận hàng hiện tại">
      {expired ? (
        <p role="status">Mã đã hết hạn. Vui lòng tạo mã mới.</p>
      ) : (
        <>
          <div className="pickup-qr-surface">
            <canvas ref={ref} aria-label="Mã QR nhận hàng" role="img" />
          </div>
          <p className="pickup-expiry">
            <Clock3 aria-hidden="true" size={16} /> Hết hạn{" "}
            {dateTime(credential.expiresAt)}
          </p>
          <details className="pickup-manual-code">
            <summary>Xem mã để nhập thủ công</summary>
            <code>{credential.token}</code>
          </details>
        </>
      )}
      {!!error && <FeatureError error={error} />}
    </section>
  );
}
export function CustomerPickup({ orderId }: { orderId: string }) {
  const [credential, setCredential] = useState<PickupCredential>();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    setCredential(undefined);
    setError(undefined);
  }, [orderId]);
  async function issue() {
    setBusy(true);
    setError(undefined);
    setCredential(undefined);
    try {
      setCredential(await issuePickup(orderId));
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section
      className="feature-panel customer-pickup"
      aria-labelledby="customer-pickup-title"
    >
      <div className="panel-heading">
        <QrCode aria-hidden="true" size={22} />
        <h2 id="customer-pickup-title">Nhận hàng bằng QR</h2>
      </div>
      <p className="panel-description">
        Đưa mã QR cho nhân viên khi đến nhận hàng. Mã chỉ có hiệu lực trong thời
        gian hiển thị.
      </p>
      <button
        className="feature-button pickup-issue"
        disabled={busy}
        onClick={issue}
      >
        {busy ? "Đang tạo…" : credential ? "Tạo mã mới" : "Tạo mã nhận hàng"}
      </button>
      <p className="pickup-help">Mỗi lần tạo mã sẽ vô hiệu mã cũ.</p>
      {!!error && <FeatureError error={error} />}
      {credential && <PickupQR credential={credential} />}
    </section>
  );
}
export function OrderHistoryPanel({
  orderId,
  orgId,
}: {
  orderId: string;
  orgId?: string;
}) {
  const user = useAuthStore((s) => s.user);
  const [page, setPage] = useState(0);
  const q = useQuery({
    queryKey: ["order-history", user?.id, orgId, orderId, page],
    queryFn: () => getOrderHistory(orderId, page, orgId),
    enabled: !!user,
  });
  return (
    <section className="feature-panel order-history">
      <div className="panel-heading">
        <History aria-hidden="true" size={22} />
        <h2>Lịch sử đơn hàng</h2>
      </div>
      {q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data?.content.length === 0 && (
        <p className="panel-description">
          Chưa có cập nhật nào cho đơn hàng này.
        </p>
      )}
      <ol className="order-history-list">
        {q.data?.content.map((h) => (
          <li key={h.id}>
            <span className="history-marker">
              <Check aria-hidden="true" size={14} />
            </span>
            <div>
              <p className="font-semibold text-black-blue">
                {h.source === "PICKUP_SCHEDULE"
                  ? "Đã cập nhật lịch nhận hàng"
                  : h.source === "CAMPAIGN"
                    ? "Đã cập nhật đơn đặt trước"
                    : ((
                        {
                          PENDING: "Chờ xác nhận",
                          CONFIRMED: "Đã xác nhận đơn hàng",
                          READY: "Đơn hàng sẵn sàng nhận",
                          COMPLETED: "Đã nhận hàng",
                          CANCELLED: "Đã hủy đơn hàng",
                        } as Record<string, string>
                      )[h.toStatus] ?? "Đã cập nhật đơn hàng")}
              </p>
              <time className="text-sm text-slate" dateTime={h.createdAt}>
                {dateTime(h.createdAt)}
              </time>
            </div>
          </li>
        ))}
      </ol>
      {q.data && (
        <PageControls page={page} total={q.data.totalPages} change={setPage} />
      )}
    </section>
  );
}
export function PickupScanner({ orgId }: { orgId: string }) {
  const video = useRef<HTMLVideoElement>(null);
  const controls = useRef<IScannerControls>();
  const generation = useRef(0);
  const [token, setToken] = useState("");
  const [schedule, setSchedule] = useState("");
  const [order, setOrder] = useState<OrderResponse>();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const [camera, setCamera] = useState(false);
  function stop() {
    generation.current++;
    controls.current?.stop();
    controls.current = undefined;
    if (video.current) {
      const stream = video.current.srcObject as MediaStream | null;
      stream?.getTracks().forEach((t) => t.stop());
      video.current.srcObject = null;
    }
    setCamera(false);
  }
  useEffect(() => {
    setToken("");
    setSchedule("");
    setOrder(undefined);
    setError(undefined);
    return stop;
  }, [orgId]);
  async function start() {
    stop();
    const attempt = ++generation.current;
    setError(undefined);
    setCamera(true);
    try {
      const { BrowserQRCodeReader } = await import("@zxing/browser");
      if (generation.current !== attempt) return;
      const reader = new BrowserQRCodeReader();
      const active = await reader.decodeFromVideoDevice(
        undefined,
        video.current!,
        (result) => {
          if (result && generation.current === attempt) {
            setToken(result.getText());
            setOrder(undefined);
            stop();
          }
        },
      );
      if (generation.current === attempt) controls.current = active;
      else active.stop();
    } catch (e) {
      if (generation.current === attempt) {
        stop();
        setError(
          new Error(
            `Không mở được camera. Hãy nhập mã thủ công. ${featureError(e)}`,
          ),
        );
      }
    }
  }
  async function verify() {
    setBusy(true);
    setError(undefined);
    setOrder(undefined);
    try {
      setOrder(
        await verifyPickup(orgId, token.trim(), schedule.trim() || null),
      );
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  async function complete() {
    if (!order) return;
    setBusy(true);
    setError(undefined);
    try {
      const result = await completePickup(
        orgId,
        token.trim(),
        schedule.trim() || null,
      );
      setOrder(result);
      setToken("");
      invalidateFeatures();
    } catch (e) {
      setError(e);
      setOrder(undefined);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="feature-panel">
      <h2>Quét mã nhận hàng</h2>
      <div className="feature-fields">
        <label>
          Mã nhận hàng
          <input
            value={token}
            disabled={busy}
            onChange={(e) => {
              setToken(e.target.value);
              setOrder(undefined);
            }}
            autoComplete="off"
          />
        </label>
        <label>
          ID lịch nhận (tuỳ chọn)
          <input
            value={schedule}
            disabled={busy}
            onChange={(e) => {
              setSchedule(e.target.value);
              setOrder(undefined);
            }}
          />
        </label>
      </div>
      <div className="feature-actions">
        <button disabled={busy || camera} onClick={start}>
          Mở camera
        </button>
        {camera && <button onClick={stop}>Dừng camera</button>}
        <button disabled={busy || !token.trim()} onClick={verify}>
          Kiểm tra mã
        </button>
      </div>
      <video
        ref={video}
        playsInline
        muted
        hidden={!camera}
        className="max-w-full"
      />
      {!!error && <FeatureError error={error} />}
      {order && (
        <div>
          <p>
            Đơn #{order.id.slice(0, 8)} · {order.guestName ?? "Khách hàng"} ·{" "}
            {order.status}
          </p>
          {order.items?.map((i) => (
            <p key={i.id}>
              {i.merchName} × {i.quantity}
            </p>
          ))}
          <div className="feature-actions">
            <button
              disabled={busy || order.status !== "READY"}
              onClick={complete}
            >
              Xác nhận giao hàng
            </button>
          </div>
          {order.status === "COMPLETED" && (
            <p role="status">Đã giao hàng thành công.</p>
          )}
          <OrderHistoryPanel orderId={order.id} orgId={orgId} />
        </div>
      )}
    </section>
  );
}
export function GuestOrdersPage() {
  const [params, setParams] = useSearchParams();
  const [id, setId] = useState(params.get("orderId") ?? "");
  const [email, setEmail] = useState("");
  const [receipt, setReceipt] = useState(params.get("receiptToken") ?? "");
  const [order, setOrder] = useState<OrderResponse>();
  const [credential, setCredential] = useState<PickupCredential>();
  const [error, setError] = useState<unknown>();
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (params.has("receiptToken") || params.has("email")) {
      const clean = new URLSearchParams(params);
      clean.delete("receiptToken");
      clean.delete("email");
      setParams(clean, { replace: true });
    }
  }, [params, setParams]);
  async function run(kind: "track" | "receipt" | "exchange") {
    setBusy(true);
    setError(undefined);
    setMessage("");
    setCredential(undefined);
    try {
      if (kind === "track") {
        setOrder(undefined);
        setOrder(await trackGuestOrder(id.trim(), email.trim()));
      } else if (kind === "receipt") {
        await requestGuestReceipt(id.trim(), email.trim());
        setMessage(
          "Nếu thông tin hợp lệ và đơn sẵn sàng nhận, hướng dẫn sẽ được gửi đến email của bạn.",
        );
      } else {
        setCredential(await exchangeGuestReceipt(id.trim(), receipt.trim()));
        setReceipt("");
      }
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <FeatureFrame title="Tra cứu đơn khách">
      <section className="feature-panel">
        <div className="feature-fields">
          <label>
            ID đơn hàng
            <input
              disabled={busy}
              value={id}
              onChange={(e) => {
                setId(e.target.value);
                setOrder(undefined);
                setCredential(undefined);
              }}
            />
          </label>
          <label>
            Email đặt hàng
            <input
              type="email"
              disabled={busy}
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setOrder(undefined);
              }}
            />
          </label>
        </div>
        <div className="feature-actions">
          <button disabled={busy || !id || !email} onClick={() => run("track")}>
            Tra cứu
          </button>
          <button
            disabled={busy || !id || !email}
            onClick={() => run("receipt")}
          >
            Gửi hướng dẫn nhận hàng
          </button>
        </div>
        {message && <p role="status">{message}</p>}
        {!!error && <FeatureError error={error} />}
        {order && (
          <div>
            <p>Trạng thái: {order.status}</p>
            {order.items?.map((i) => (
              <p key={i.id}>
                {i.merchName} × {i.quantity}
              </p>
            ))}
          </div>
        )}
        <label className="feature-fields">
          Mã xác nhận từ email
          <input
            type="password"
            autoComplete="off"
            disabled={busy}
            value={receipt}
            onChange={(e) => setReceipt(e.target.value)}
          />
        </label>
        <div className="feature-actions">
          <button
            disabled={busy || !id || !receipt}
            onClick={() => run("exchange")}
          >
            Đổi mã nhận hàng
          </button>
        </div>
        {credential && <PickupQR credential={credential} />}
      </section>
    </FeatureFrame>
  );
}
