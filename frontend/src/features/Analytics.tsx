import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getAnalytics } from "../api/analytics";
import { campusDate, money, percentage } from "../lib/featureUtils";
import { useAuthStore } from "../stores/authStore";
import { FeatureError, FeatureLoading } from "./FeatureUI";
export function AnalyticsPanel({ orgId }: { orgId: string }) {
  const user = useAuthStore((s) => s.user);
  const [from, setFrom] = useState(
    campusDate(new Date(Date.now() - 29 * 86400000)),
  );
  const [to, setTo] = useState(campusDate());
  const days = (Date.parse(to) - Date.parse(from)) / 86400000 + 1;
  const valid = !!from && !!to && days >= 1 && days <= 366;
  const q = useQuery({
    queryKey: ["analytics", user?.id, orgId, from, to],
    queryFn: () => getAnalytics(orgId, from, to),
    enabled: valid,
  });
  return (
    <section className="feature-panel">
      <h2>Thống kê tổ chức</h2>
      <div className="feature-fields">
        <label>
          Từ ngày
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
        </label>
        <label>
          Đến ngày
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </label>
      </div>
      {!valid && <p role="alert">Chọn khoảng ngày hợp lệ, tối đa 366 ngày.</p>}
      {valid && q.isPending && <FeatureLoading />}
      {q.isError && <FeatureError error={q.error} retry={() => q.refetch()} />}
      {q.data && valid && (
        <>
          <p>
            Số liệu đơn theo ngày tạo và trạng thái hiện tại; tồn kho là số liệu
            hiện tại. Giá trị đơn hoàn thành khác với số tiền đã thanh toán.
          </p>
          <div className="feature-actions">
            {[
              ["Tổng đơn", q.data.orders.total],
              ["Đơn hoàn thành", q.data.orders.completed],
              [
                "Giá trị đơn hoàn thành",
                money(q.data.orders.completedOrderValue),
              ],
              ["Giá trị đã thanh toán", money(q.data.orders.paidOrderValue)],
              ["Tỷ lệ huỷ", percentage(q.data.orders.cancellationRate)],
              ["Tồn kho", q.data.inventory.availableUnits],
            ].map(([label, value]) => (
              <div className="feature-panel" key={label}>
                <p>{label}</p>
                <strong>{value}</strong>
              </div>
            ))}
          </div>
          <h3>Đơn theo ngày</h3>
          <div className="overflow-x-auto">
            <table className="feature-table">
              <thead>
                <tr>
                  <th>Ngày</th>
                  <th>Tổng</th>
                  <th>Hoàn thành</th>
                  <th>Huỷ</th>
                  <th>Giá trị hoàn thành</th>
                </tr>
              </thead>
              <tbody>
                {q.data.dailyOrders.map((d) => (
                  <tr key={d.date}>
                    <td>{d.date}</td>
                    <td>{d.orders}</td>
                    <td>{d.completed}</td>
                    <td>{d.cancelled}</td>
                    <td>{money(d.completedOrderValue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <h3>Vật phẩm bán nhiều</h3>
          {q.data.topProducts.length === 0 ? (
            <p>Chưa có đơn hoàn thành.</p>
          ) : (
            q.data.topProducts.map((p) => (
              <p key={p.merchId}>
                {p.name} · {p.quantity} sản phẩm ·{" "}
                {money(p.completedOrderValue)}
              </p>
            ))
          )}
          <h3>Khối lượng nhận hàng</h3>
          {q.data.pickupWorkload.length === 0 ? (
            <p>Chưa có lịch nhận trong khoảng ngày này.</p>
          ) : (
            q.data.pickupWorkload.map((p) => (
              <p key={p.scheduleId}>
                {p.date} {p.timeSlot} · Chờ nhận {p.ready} · Đã nhận{" "}
                {p.completed}
              </p>
            ))
          )}
        </>
      )}
    </section>
  );
}
