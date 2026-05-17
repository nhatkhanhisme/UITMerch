import { lazy, Suspense, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { getApiErrorMessage } from "../api/auth";
import {
  attachMerchToEvent,
  createEvent,
  deleteEvent,
  detachMerchFromEvent,
  getOwnEvents,
  updateEvent,
} from "../api/event";
import {
  createMerch,
  deleteMerch,
  getCategories,
  getOwnMerch,
  updateMerch,
} from "../api/merch";
import {
  createOrganization,
  getOwnOrganizations,
  updateOrganization,
} from "../api/organization";
import { getOrgOrders, updateOrgOrderStatus } from "../api/order";
import { uploadEventImage, uploadMerchImage, uploadOrganizerImage } from "../api/storage";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { CategoryResponse } from "../types/shared";
import type {
  EventResponse,
  MerchResponse,
  OrderResponse,
  OrganizationResponse,
} from "../types/shared";

const ShaderBackground = lazy(() =>
  import("../components/ui/ShaderBackground").then((m) => ({
    default: m.ShaderBackground,
  })),
);

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  currency: "VND",
  style: "currency",
});
function formatPrice(p?: number | null) {
  if (p == null) return "—";
  return currencyFormatter.format(p);
}
function formatDate(d?: string) {
  if (!d) return "—";
  return new Date(d).toLocaleDateString("vi-VN");
}

type Tab = "merch" | "events" | "orders" | "profile";

const ORDER_STATUS_TRANSITIONS: Record<string, string[]> = {
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["READY_FOR_PICKUP", "CANCELLED"],
  READY_FOR_PICKUP: ["SUCCESS"],
  SUCCESS: [],
  CANCELLED: [],
};

const ORDER_NEXT_LABELS: Record<string, string> = {
  CONFIRMED: "Xác nhận đơn",
  READY_FOR_PICKUP: "Sẵn sàng lấy hàng",
  SUCCESS: "Hoàn thành đơn",
  CANCELLED: "Huỷ đơn",
};

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  READY_FOR_PICKUP: "Sẵn sàng",
  SUCCESS: "Hoàn thành",
  CANCELLED: "Đã huỷ",
  DRAFT: "Nháp",
  PUBLISHED: "Công bố",
  ENDED: "Đã kết thúc",
  ARCHIVED: "Đã ẩn",
  ACTIVE: "Hoạt động",
  INACTIVE: "Tạm dừng",
  PENDING_ORG: "Chờ duyệt",
};

// ─── Org Selector ──────────────────────────────────────────────────────────────

function CreateOrgForm({ onCreated }: { onCreated: (org: OrganizationResponse) => void }) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setIsSubmitting(true);
    try {
      const res = await createOrganization({ name: name.trim(), description: description.trim() || undefined });
      toast.success("Tổ chức đã được tạo và đang chờ duyệt!");
      onCreated(res.data);
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="space-y-4 rounded-panel border border-white/50 bg-white/45 p-6 shadow-glass backdrop-blur-xl" onSubmit={handleSubmit}>
      <h3 className="font-fredoka text-xl font-bold text-black-blue">Tạo tổ chức mới</h3>
      <div>
        <label className="block text-xs font-semibold text-ink/70 mb-1">Tên tổ chức *</label>
        <input
          className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
          onChange={e => setName(e.target.value)}
          placeholder="Ví dụ: CLB Lập trình UIT"
          required
          value={name}
        />
      </div>
      <div>
        <label className="block text-xs font-semibold text-ink/70 mb-1">Mô tả</label>
        <textarea
          className="h-20 w-full resize-none rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
          onChange={e => setDescription(e.target.value)}
          placeholder="Giới thiệu ngắn về tổ chức..."
          value={description}
        />
      </div>
      <button
        className="rounded-full bg-black-blue px-6 py-2.5 text-sm font-bold text-white transition hover:bg-ink disabled:opacity-50"
        disabled={isSubmitting}
        type="submit"
      >
        {isSubmitting ? "Đang tạo..." : "Tạo tổ chức"}
      </button>
      <p className="text-xs text-ink/50">Tổ chức mới sẽ có trạng thái PENDING, chờ admin duyệt trước khi bán được hàng.</p>
    </form>
  );
}

// ─── Merch Tab ─────────────────────────────────────────────────────────────────

function MerchTab({ orgId }: { orgId: string }) {
  const [items, setItems] = useState<MerchResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState<MerchResponse | null>(null);
  const [form, setForm] = useState({
    name: "", description: "", price: "", stock: "0", status: "DRAFT",
    categoryId: "", imageUrls: [] as string[],
  });
  const [isUploadingImage, setIsUploadingImage] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmHideId, setConfirmHideId] = useState<string | null>(null);
  const [hidingId, setHidingId] = useState<string | null>(null);

  const load = () => {
    setIsLoading(true);
    Promise.all([getOwnMerch(orgId), getCategories()])
      .then(([mrRes, catRes]) => {
        setItems(mrRes.data ?? []);
        setCategories(catRes.data ?? []);
      })
      .catch(() => toast.error("Không thể tải danh sách vật phẩm."))
      .finally(() => setIsLoading(false));
  };

  useEffect(() => { load(); }, [orgId]);

  const openCreate = () => {
    setEditingItem(null);
    setForm({ name: "", description: "", price: "", stock: "0", status: "DRAFT", categoryId: "", imageUrls: [] });
    setShowForm(true);
  };

  const openEdit = (item: MerchResponse) => {
    setEditingItem(item);
    setForm({
      name: item.name,
      description: item.description ?? "",
      price: item.price?.toString() ?? "",
      stock: item.stock.toString(),
      status: item.status,
      categoryId: item.categoryId ?? "",
      imageUrls: item.images ?? [],
    });
    setShowForm(true);
  };

  const handleImageFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    if (form.imageUrls.length >= 5) { toast.error("Tối đa 5 ảnh."); return; }
    setIsUploadingImage(true);
    try {
      const url = await uploadMerchImage(file, orgId);
      setForm(f => ({ ...f, imageUrls: [...f.imageUrls, url] }));
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Không thể tải ảnh lên."));
    } finally {
      setIsUploadingImage(false);
    }
  };

  const removeImageUrl = (url: string) => {
    setForm(f => ({ ...f, imageUrls: f.imageUrls.filter(u => u !== url) }));
  };

  const adjustPrice = (delta: number) => {
    setForm(f => {
      const current = parseFloat(f.price) || 0;
      const next = Math.max(0, current + delta);
      return { ...f, price: next === 0 ? "" : String(next) };
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) return;
    setIsSubmitting(true);
    try {
      if (editingItem) {
        await updateMerch(orgId, editingItem.id, {
          name: form.name.trim(),
          description: form.description.trim() || undefined,
          price: form.price ? parseFloat(form.price) : undefined,
          stock: parseInt(form.stock, 10),
          status: form.status,
          categoryId: form.categoryId || undefined,
          imageUrls: form.imageUrls.length > 0 ? form.imageUrls : undefined,
        });
        toast.success("Đã cập nhật vật phẩm.");
      } else {
        await createMerch(orgId, {
          name: form.name.trim(),
          description: form.description.trim() || undefined,
          price: form.price ? parseFloat(form.price) : undefined,
          stock: parseInt(form.stock, 10),
          categoryId: form.categoryId || undefined,
          imageUrls: form.imageUrls.length > 0 ? form.imageUrls : undefined,
        });
        toast.success("Đã tạo vật phẩm.");
      }
      setShowForm(false);
      load();
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleHide = async (item: MerchResponse) => {
    setHidingId(item.id);
    try {
      await deleteMerch(orgId, item.id);
      setItems(prev => prev.map(i => i.id === item.id ? { ...i, status: "ARCHIVED" } : i));
      setConfirmHideId(null);
      toast.info(`Đã ẩn "${item.name}".`);
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setHidingId(null);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="font-fredoka text-2xl font-bold text-black-blue">Vật phẩm</h2>
        <button
          className="rounded-full bg-black-blue px-5 py-2 text-sm font-bold text-white transition hover:bg-ink"
          onClick={openCreate}
          type="button"
        >
          + Thêm vật phẩm
        </button>
      </div>

      {showForm && (
        <form className="rounded-panel border border-aqua/50 bg-white/60 p-6 shadow-glass backdrop-blur-xl space-y-4" onSubmit={handleSubmit}>
          <h3 className="font-fredoka text-lg font-bold text-black-blue">
            {editingItem ? "Chỉnh sửa vật phẩm" : "Thêm vật phẩm mới"}
          </h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Tên *</label>
              <input
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                required
                value={form.name}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Giá (VND)</label>
              <div className="flex items-center gap-1">
                <button className="size-9 rounded-xl border border-white/70 bg-white/50 text-lg font-bold text-black-blue hover:bg-white/80 transition flex-none" onClick={() => adjustPrice(-1000)} type="button">−</button>
                <input
                  className="flex-1 rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua text-center"
                  min="0"
                  onChange={e => setForm(f => ({ ...f, price: e.target.value }))}
                  placeholder="0"
                  type="number"
                  value={form.price}
                />
                <button className="size-9 rounded-xl border border-white/70 bg-white/50 text-lg font-bold text-black-blue hover:bg-white/80 transition flex-none" onClick={() => adjustPrice(1000)} type="button">+</button>
              </div>
              <div className="mt-1 flex gap-1">
                {[5000,10000,50000,100000].map(v => (
                  <button className="rounded-full border border-white/60 bg-white/40 px-2 py-0.5 text-[10px] font-semibold text-black-blue hover:bg-white/70" key={v} onClick={() => setForm(f => ({ ...f, price: String(v) }))} type="button">
                    +{v >= 1000 ? `${v/1000}k` : v}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Tồn kho *</label>
              <input
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                min="0"
                onChange={e => setForm(f => ({ ...f, stock: e.target.value }))}
                required
                type="number"
                value={form.stock}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Danh mục</label>
              <select
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={e => setForm(f => ({ ...f, categoryId: e.target.value }))}
                value={form.categoryId}
              >
                <option value="">— Không có danh mục —</option>
                {categories.map(cat => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>
            </div>
            {editingItem && (
              <div>
                <label className="block text-xs font-semibold text-ink/70 mb-1">Trạng thái</label>
                <select
                  className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                  onChange={e => setForm(f => ({ ...f, status: e.target.value }))}
                  value={form.status}
                >
                  <option value="DRAFT">Nháp</option>
                  <option value="PUBLISHED">Đang bán</option>
                  <option value="ARCHIVED">Đã ẩn</option>
                </select>
              </div>
            )}
          </div>
          <div>
            <label className="block text-xs font-semibold text-ink/70 mb-1">Mô tả</label>
            <textarea
              className="h-20 w-full resize-none rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
              onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
              value={form.description}
            />
          </div>

          {/* Image upload */}
          <div>
            <label className="block text-xs font-semibold text-ink/70 mb-2">Ảnh sản phẩm (tối đa 5)</label>
            <div className="flex flex-wrap gap-2 mb-3">
              {form.imageUrls.map((url, idx) => (
                <div className="relative group" key={idx}>
                  <img
                    alt={`Ảnh ${idx + 1}`}
                    className="size-16 rounded-xl object-cover border border-white/60 bg-white/30"
                    onError={e => { (e.target as HTMLImageElement).src = "https://placehold.co/64x64/e9feff/1a3a4a?text=?"; }}
                    src={url}
                  />
                  <button
                    className="absolute -top-1.5 -right-1.5 size-5 rounded-full bg-peach border border-white text-xs font-bold text-black-blue hidden group-hover:flex items-center justify-center transition"
                    onClick={() => removeImageUrl(url)}
                    type="button"
                  >
                    ✕
                  </button>
                </div>
              ))}
              {form.imageUrls.length < 5 && (
                <label className={[
                  "size-16 rounded-xl border-2 border-dashed border-white/60 bg-white/30 flex flex-col items-center justify-center cursor-pointer hover:border-aqua hover:bg-aqua/10 transition",
                  isUploadingImage ? "opacity-50 pointer-events-none" : "",
                ].join(" ")}>
                  <span className="text-xl text-ink/40">{isUploadingImage ? "⏳" : "+"}</span>
                  <span className="text-[9px] text-ink/50 mt-0.5">{isUploadingImage ? "Đang tải" : "Thêm ảnh"}</span>
                  <input
                    accept="image/*"
                    className="hidden"
                    disabled={isUploadingImage}
                    onChange={handleImageFileUpload}
                    type="file"
                  />
                </label>
              )}
            </div>
            <p className="text-xs text-ink/40">Tải ảnh từ máy tính. Tối đa 5 ảnh, mỗi ảnh ≤ 10 MB.</p>
          </div>

          <div className="flex gap-2">
            <button
              className="rounded-full bg-black-blue px-5 py-2 text-sm font-bold text-white transition hover:bg-ink disabled:opacity-50"
              disabled={isSubmitting}
              type="submit"
            >
              {isSubmitting ? "Đang lưu..." : "Lưu"}
            </button>
            <button
              className="rounded-full border border-ink/20 bg-white/50 px-5 py-2 text-sm font-semibold text-black-blue transition hover:bg-white"
              onClick={() => setShowForm(false)}
              type="button"
            >
              Huỷ
            </button>
          </div>
        </form>
      )}

      {isLoading ? (
        <div className="py-12 text-center text-sm text-ink/60">Đang tải...</div>
      ) : items.length === 0 ? (
        <div className="py-12 text-center rounded-panel border border-white/40 bg-white/30">
          <p className="text-sm text-ink/60">Chưa có vật phẩm nào. Bấm "Thêm vật phẩm" để bắt đầu.</p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-panel border border-white/50 bg-white/35 shadow-glass backdrop-blur-xl">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-white/40 bg-white/20">
                <th className="px-3 py-3 text-left text-xs font-semibold uppercase text-ink/50 w-14">Ảnh</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Tên</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Giá</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Tồn</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-ink/50">Trạng thái</th>
                <th className="px-4 py-3 text-right text-xs font-semibold uppercase text-ink/50">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/30">
              {items.map(item => (
                <tr className="hover:bg-white/20" key={item.id}>
                  <td className="px-3 py-2">
                    {item.images && item.images.length > 0 ? (
                      <img
                        alt={item.name}
                        className="size-10 rounded-lg object-cover border border-white/60"
                        onError={e => { (e.target as HTMLImageElement).src = "https://placehold.co/40x40/e9feff/1a3a4a?text=?"; }}
                        src={item.images[0]}
                      />
                    ) : (
                      <div className="size-10 rounded-lg bg-white/50 border border-white/60 flex items-center justify-center text-ink/30 text-lg">
                        📦
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3 font-semibold text-black-blue">{item.name}</td>
                  <td className="px-4 py-3 text-ink/70">{formatPrice(item.price)}</td>
                  <td className="px-4 py-3 text-ink/70">{item.stock}</td>
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-white/50 border border-white/60 px-2.5 py-0.5 text-xs font-semibold text-black-blue">
                      {STATUS_LABELS[item.status] ?? item.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    {confirmHideId === item.id ? (
                      <div className="flex justify-end items-center gap-2">
                        <span className="text-xs text-ink/60">Ẩn vật phẩm?</span>
                        <button
                          className="rounded-full border border-peach/60 bg-peach/20 px-3 py-1 text-xs font-bold text-black-blue hover:bg-peach/40 disabled:opacity-50"
                          disabled={hidingId === item.id}
                          onClick={() => handleHide(item)}
                          type="button"
                        >
                          {hidingId === item.id ? "..." : "Có"}
                        </button>
                        <button
                          className="rounded-full border border-white/60 bg-white/50 px-3 py-1 text-xs font-semibold text-black-blue hover:bg-white"
                          onClick={() => setConfirmHideId(null)}
                          type="button"
                        >
                          Không
                        </button>
                      </div>
                    ) : (
                      <div className="flex justify-end gap-2">
                        <button
                          className="rounded-full border border-aqua/50 bg-aqua/10 px-3 py-1 text-xs font-semibold text-black-blue hover:bg-aqua/30"
                          onClick={() => openEdit(item)}
                          type="button"
                        >
                          Sửa
                        </button>
                        <button
                          className="rounded-full border border-peach/50 bg-peach/10 px-3 py-1 text-xs font-semibold text-black-blue hover:bg-peach/30 disabled:opacity-40"
                          disabled={item.status === "ARCHIVED"}
                          onClick={() => setConfirmHideId(item.id)}
                          type="button"
                        >
                          Ẩn
                        </button>
                      </div>
                    )}
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

// ─── Events Tab ────────────────────────────────────────────────────────────────

function EventsTab({ orgId }: { orgId: string }) {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [orgMerch, setOrgMerch] = useState<MerchResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingEvent, setEditingEvent] = useState<EventResponse | null>(null);
  const [form, setForm] = useState({ title: "", description: "", status: "DRAFT", startsAt: "", endsAt: "", coverUrl: "" });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isUploadingCover, setIsUploadingCover] = useState(false);
  const [attachingId, setAttachingId] = useState<string | null>(null);
  const [confirmDeleteEventId, setConfirmDeleteEventId] = useState<string | null>(null);
  const [deletingEventId, setDeletingEventId] = useState<string | null>(null);

  const load = () => {
    setIsLoading(true);
    Promise.all([
      getOwnEvents(orgId),
      getOwnMerch(orgId),
    ])
      .then(([evRes, mrRes]) => {
        setEvents(evRes.data ?? []);
        setOrgMerch((mrRes.data ?? []).filter(m => m.status === "PUBLISHED" || m.status === "DRAFT"));
      })
      .catch(() => toast.error("Không thể tải dữ liệu sự kiện."))
      .finally(() => setIsLoading(false));
  };

  useEffect(() => { load(); }, [orgId]);

  const openCreate = () => {
    setEditingEvent(null);
    setForm({ title: "", description: "", status: "DRAFT", startsAt: "", endsAt: "", coverUrl: "" });
    setShowForm(true);
  };

  const openEdit = (event: EventResponse) => {
    setEditingEvent(event);
    setForm({
      title: event.title,
      description: event.description ?? "",
      status: event.status,
      startsAt: event.startsAt ? event.startsAt.slice(0, 16) : "",
      endsAt: event.endsAt ? event.endsAt.slice(0, 16) : "",
      coverUrl: event.coverUrl ?? "",
    });
    setShowForm(true);
  };

  const handleCoverUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setIsUploadingCover(true);
    try {
      const url = await uploadEventImage(file, orgId);
      setForm(f => ({ ...f, coverUrl: url }));
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Không thể tải ảnh bìa lên."));
    } finally {
      setIsUploadingCover(false);
    }
  };

  const handleDeleteEvent = async (eventId: string) => {
    setDeletingEventId(eventId);
    try {
      await deleteEvent(orgId, eventId);
      setEvents(prev => prev.filter(e => e.id !== eventId));
      if (editingEvent?.id === eventId) { setShowForm(false); setEditingEvent(null); }
      setConfirmDeleteEventId(null);
      toast.info("Đã xoá sự kiện.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setDeletingEventId(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) return;
    setIsSubmitting(true);
    try {
      if (editingEvent) {
        const updated = await updateEvent(orgId, editingEvent.id, {
          title: form.title.trim(),
          description: form.description.trim() || undefined,
          status: form.status,
          startsAt: form.startsAt || undefined,
          endsAt: form.endsAt || undefined,
          coverUrl: form.coverUrl || undefined,
        });
        // Preserve local merch state — server response may not include it
        const merged = { ...updated.data, merch: editingEvent.merch ?? updated.data.merch };
        setEvents(prev => prev.map(e => e.id === editingEvent.id ? merged : e));
        setEditingEvent(merged);
        toast.success("Đã cập nhật sự kiện.");
      } else {
        await createEvent(orgId, {
          title: form.title.trim(),
          description: form.description.trim() || undefined,
          startsAt: form.startsAt || undefined,
          endsAt: form.endsAt || undefined,
        });
        toast.success("Đã tạo sự kiện.");
        setShowForm(false);
        load();
      }
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAttach = async (merchId: string) => {
    if (!editingEvent) return;
    const merch = orgMerch.find(m => m.id === merchId);
    if (!merch) return;
    // Optimistic update
    const optimistic = { ...editingEvent, merch: [...(editingEvent.merch ?? []), merch] };
    setEditingEvent(optimistic);
    setEvents(prev => prev.map(e => e.id === editingEvent.id ? optimistic : e));
    setAttachingId(merchId);
    try {
      await attachMerchToEvent(orgId, editingEvent.id, merchId);
      toast.success("Đã thêm vật phẩm vào sự kiện.");
    } catch (err) {
      // Rollback
      setEditingEvent(editingEvent);
      setEvents(prev => prev.map(e => e.id === editingEvent.id ? editingEvent : e));
      toast.error(getApiErrorMessage(err));
    } finally {
      setAttachingId(null);
    }
  };

  const handleDetach = async (merchId: string) => {
    if (!editingEvent) return;
    // Optimistic update
    const optimistic = { ...editingEvent, merch: (editingEvent.merch ?? []).filter(m => m.id !== merchId) };
    setEditingEvent(optimistic);
    setEvents(prev => prev.map(e => e.id === editingEvent.id ? optimistic : e));
    setAttachingId(merchId);
    try {
      await detachMerchFromEvent(orgId, editingEvent.id, merchId);
      toast.info("Đã gỡ vật phẩm khỏi sự kiện.");
    } catch (err) {
      // Rollback
      setEditingEvent(editingEvent);
      setEvents(prev => prev.map(e => e.id === editingEvent.id ? editingEvent : e));
      toast.error(getApiErrorMessage(err));
    } finally {
      setAttachingId(null);
    }
  };

  const attachedIds = new Set((editingEvent?.merch ?? []).map(m => m.id));

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="font-fredoka text-2xl font-bold text-black-blue">Sự kiện</h2>
        <button
          className="rounded-full bg-black-blue px-5 py-2 text-sm font-bold text-white transition hover:bg-ink"
          onClick={openCreate}
          type="button"
        >
          + Tạo sự kiện
        </button>
      </div>

      {showForm && (
        <div className="rounded-panel border border-aqua/50 bg-white/60 p-6 shadow-glass backdrop-blur-xl space-y-5">
          <h3 className="font-fredoka text-lg font-bold text-black-blue">
            {editingEvent ? "Chỉnh sửa sự kiện" : "Tạo sự kiện mới"}
          </h3>

          <form className="space-y-4" onSubmit={handleSubmit}>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Tên sự kiện *</label>
              <input
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
                required
                value={form.title}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-xs font-semibold text-ink/70 mb-1">Bắt đầu</label>
                <input
                  className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                  onChange={e => setForm(f => ({ ...f, startsAt: e.target.value }))}
                  type="datetime-local"
                  value={form.startsAt}
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-ink/70 mb-1">Kết thúc</label>
                <input
                  className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                  onChange={e => setForm(f => ({ ...f, endsAt: e.target.value }))}
                  type="datetime-local"
                  value={form.endsAt}
                />
              </div>
            </div>
            {editingEvent && (
              <div>
                <label className="block text-xs font-semibold text-ink/70 mb-1">Trạng thái</label>
                <select
                  className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                  onChange={e => setForm(f => ({ ...f, status: e.target.value }))}
                  value={form.status}
                >
                  <option value="DRAFT">Nháp</option>
                  <option value="PUBLISHED">Công bố</option>
                  <option value="ENDED">Đã kết thúc</option>
                </select>
              </div>
            )}
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Mô tả</label>
              <textarea
                className="h-20 w-full resize-none rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                value={form.description}
              />
            </div>
            {/* Cover image upload */}
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-2">Ảnh bìa sự kiện</label>
              {form.coverUrl && (
                <div className="relative mb-2 inline-block group">
                  <img alt="Ảnh bìa" className="h-20 rounded-xl object-cover border border-white/60" src={form.coverUrl} />
                  <button className="absolute -top-1.5 -right-1.5 size-5 rounded-full bg-peach border border-white text-xs font-bold text-black-blue hidden group-hover:flex items-center justify-center" onClick={() => setForm(f => ({ ...f, coverUrl: "" }))} type="button">✕</button>
                </div>
              )}
              <label className={["inline-flex cursor-pointer items-center gap-2 rounded-full border border-white/60 bg-white/50 px-4 py-2 text-xs font-semibold text-black-blue hover:bg-white/80 transition", isUploadingCover ? "opacity-50 pointer-events-none" : ""].join(" ")}>
                {isUploadingCover ? "⏳ Đang tải..." : "📷 Tải ảnh bìa"}
                <input accept="image/*" className="hidden" disabled={isUploadingCover} onChange={handleCoverUpload} type="file" />
              </label>
            </div>

            <div className="flex gap-2">
              <button
                className="rounded-full bg-black-blue px-5 py-2 text-sm font-bold text-white transition hover:bg-ink disabled:opacity-50"
                disabled={isSubmitting}
                type="submit"
              >
                {isSubmitting ? "Đang lưu..." : "Lưu"}
              </button>
              <button
                className="rounded-full border border-ink/20 bg-white/50 px-5 py-2 text-sm font-semibold text-black-blue transition hover:bg-white"
                onClick={() => setShowForm(false)}
                type="button"
              >
                Huỷ
              </button>
            </div>
          </form>

          {/* Merch attachment — only when editing an existing event */}
          {editingEvent && (
            <div className="border-t border-white/40 pt-5">
              <p className="font-fredoka text-base font-bold text-black-blue mb-3">
                Vật phẩm của sự kiện
              </p>

              {/* Currently attached */}
              {(editingEvent.merch ?? []).length > 0 && (
                <div className="mb-4 space-y-2">
                  <p className="text-xs font-semibold text-ink/50 uppercase">Đang gắn ({editingEvent.merch?.length})</p>
                  <div className="flex flex-wrap gap-2">
                    {(editingEvent.merch ?? []).map(m => (
                      <div
                        className="flex items-center gap-2 rounded-full border border-aqua/60 bg-aqua/15 pl-2 pr-1 py-1"
                        key={m.id}
                      >
                        {m.images?.[0] && (
                          <img alt="" className="size-5 rounded-full object-cover" src={m.images[0]} />
                        )}
                        <span className="text-xs font-semibold text-black-blue">{m.name}</span>
                        <button
                          className="size-5 rounded-full bg-peach/40 text-xs font-bold text-black-blue hover:bg-peach/70 flex items-center justify-center disabled:opacity-50"
                          disabled={attachingId === m.id}
                          onClick={() => handleDetach(m.id)}
                          type="button"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Available to attach */}
              {orgMerch.filter(m => !attachedIds.has(m.id)).length > 0 ? (
                <div>
                  <p className="text-xs font-semibold text-ink/50 uppercase mb-2">Thêm vật phẩm</p>
                  <div className="flex flex-wrap gap-2">
                    {orgMerch.filter(m => !attachedIds.has(m.id)).map(m => (
                      <button
                        className="flex items-center gap-2 rounded-full border border-white/60 bg-white/50 pl-2 pr-3 py-1 text-xs font-semibold text-black-blue hover:border-aqua hover:bg-aqua/10 transition disabled:opacity-50"
                        disabled={attachingId === m.id}
                        key={m.id}
                        onClick={() => handleAttach(m.id)}
                        type="button"
                      >
                        {m.images?.[0] ? (
                          <img alt="" className="size-5 rounded-full object-cover" src={m.images[0]} />
                        ) : (
                          <span>📦</span>
                        )}
                        {m.name}
                        <span className="text-aqua">+</span>
                      </button>
                    ))}
                  </div>
                </div>
              ) : (
                <p className="text-xs text-ink/50">
                  {orgMerch.length === 0
                    ? "Tổ chức chưa có vật phẩm nào. Hãy thêm vật phẩm trước."
                    : "Tất cả vật phẩm đã được gắn vào sự kiện."}
                </p>
              )}
            </div>
          )}
        </div>
      )}

      {isLoading ? (
        <div className="py-12 text-center text-sm text-ink/60">Đang tải...</div>
      ) : events.length === 0 ? (
        <div className="py-12 text-center rounded-panel border border-white/40 bg-white/30">
          <p className="text-sm text-ink/60">Chưa có sự kiện nào.</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {events.map(event => (
            <div className="rounded-[28px] border border-white/50 bg-white/40 p-5 shadow-glass backdrop-blur-xl" key={event.id}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="font-fredoka text-lg font-bold text-black-blue">{event.title}</h3>
                  <p className="mt-1 text-xs text-ink/55">
                    {formatDate(event.startsAt)} – {formatDate(event.endsAt)}
                  </p>
                </div>
                <span className="rounded-full bg-white/50 border border-white/60 px-2.5 py-0.5 text-xs font-semibold text-black-blue shrink-0">
                  {STATUS_LABELS[event.status] ?? event.status}
                </span>
              </div>
              {event.description && (
                <p className="mt-2 line-clamp-2 text-xs text-ink/60">{event.description}</p>
              )}
              {event.merch && event.merch.length > 0 && (
                <p className="mt-2 text-xs text-ink/50">{event.merch.length} vật phẩm</p>
              )}
              <div className="mt-3 flex flex-wrap gap-2 items-center">
                <button
                  className="rounded-full border border-aqua/50 bg-aqua/10 px-3 py-1 text-xs font-semibold text-black-blue hover:bg-aqua/30"
                  onClick={() => openEdit(event)}
                  type="button"
                >
                  Sửa
                </button>
                {confirmDeleteEventId === event.id ? (
                  <>
                    <span className="text-xs text-ink/60">Xoá sự kiện?</span>
                    <button
                      className="rounded-full border border-peach/60 bg-peach/20 px-3 py-1 text-xs font-bold text-black-blue hover:bg-peach/40 disabled:opacity-50"
                      disabled={deletingEventId === event.id}
                      onClick={() => handleDeleteEvent(event.id)}
                      type="button"
                    >
                      {deletingEventId === event.id ? "..." : "Có"}
                    </button>
                    <button
                      className="rounded-full border border-white/60 bg-white/50 px-3 py-1 text-xs font-semibold text-black-blue hover:bg-white"
                      onClick={() => setConfirmDeleteEventId(null)}
                      type="button"
                    >
                      Không
                    </button>
                  </>
                ) : (
                  <button
                    className="rounded-full border border-peach/40 bg-white/30 px-3 py-1 text-xs font-semibold text-black-blue hover:bg-peach/20"
                    onClick={() => setConfirmDeleteEventId(event.id)}
                    type="button"
                  >
                    Xoá
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Orders Tab ────────────────────────────────────────────────────────────────

function OrgOrdersTab({ orgId }: { orgId: string }) {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState("");
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const load = () => {
    setIsLoading(true);
    getOrgOrders(orgId, { status: filterStatus || undefined, size: 50 })
      .then(res => setOrders(res.data ?? []))
      .catch(() => toast.error("Không thể tải đơn hàng."))
      .finally(() => setIsLoading(false));
  };

  useEffect(() => { load(); }, [orgId, filterStatus]);

  const handleStatusUpdate = async (orderId: string, newStatus: string) => {
    setUpdatingId(orderId);
    try {
      const res = await updateOrgOrderStatus(orgId, orderId, { status: newStatus });
      setOrders(prev => prev.map(o => o.id === orderId ? res.data : o));
      toast.success("Đã cập nhật trạng thái đơn hàng.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setUpdatingId(null);
    }
  };

  const ORG_STATUS_TABS = [
    { label: "Tất cả", value: "" },
    { label: "Chờ xác nhận", value: "PENDING" },
    { label: "Đã xác nhận", value: "CONFIRMED" },
    { label: "Sẵn sàng", value: "READY_FOR_PICKUP" },
    { label: "Hoàn thành", value: "SUCCESS" },
    { label: "Đã huỷ", value: "CANCELLED" },
  ];

  const STATUS_COLORS: Record<string, string> = {
    PENDING: "border-gold/50 bg-gold/15 text-black-blue",
    CONFIRMED: "border-aqua/50 bg-aqua/15 text-black-blue",
    READY_FOR_PICKUP: "border-aqua/70 bg-aqua/25 text-black-blue",
    SUCCESS: "border-green-400/50 bg-green-50 text-green-900",
    CANCELLED: "border-peach/50 bg-peach/15 text-black-blue",
  };

  return (
    <div className="space-y-5">
      <h2 className="font-fredoka text-2xl font-bold text-black-blue">Đơn hàng đến</h2>

      <div className="flex flex-wrap gap-2">
        {ORG_STATUS_TABS.map(tab => (
          <button
            className={[
              "rounded-full border px-4 py-1.5 text-xs font-semibold transition",
              filterStatus === tab.value
                ? "border-black-blue bg-black-blue text-white"
                : "border-white/60 bg-white/50 text-black-blue hover:border-black-blue",
            ].join(" ")}
            key={tab.value}
            onClick={() => setFilterStatus(tab.value)}
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
        <div className="flex flex-col gap-4">
          {orders.map(order => {
            const nextStatuses = ORDER_STATUS_TRANSITIONS[order.status] ?? [];
            return (
              <div className="rounded-panel border border-white/50 bg-white/40 p-5 shadow-glass backdrop-blur-xl" key={order.id}>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-0.5">
                    <p className="text-xs font-semibold text-ink/50">#{order.id.slice(0, 8).toUpperCase()}</p>
                    <p className="text-xs text-ink/40">{formatDate(order.createdAt)}</p>
                    {order.guestName && (
                      <p className="text-sm font-bold text-black-blue">{order.guestName}</p>
                    )}
                    {order.guestPhone && (
                      <p className="text-xs text-ink/60">{order.guestPhone}</p>
                    )}
                    {order.guestAddress && (
                      <p className="text-xs text-ink/60 max-w-xs">{order.guestAddress}</p>
                    )}
                    {order.note && (
                      <p className="text-xs italic text-ink/50">Ghi chú: {order.note}</p>
                    )}
                  </div>
                  <span className={[
                    "rounded-full border px-3 py-0.5 text-xs font-semibold shrink-0",
                    STATUS_COLORS[order.status] ?? "border-white/60 bg-white/50 text-black-blue",
                  ].join(" ")}>
                    {STATUS_LABELS[order.status] ?? order.status}
                  </span>
                </div>

                {order.items && order.items.length > 0 && (
                  <div className="mt-3 space-y-1 border-t border-white/30 pt-3">
                    {order.items.map(item => (
                      <div className="flex justify-between text-xs text-ink/65" key={item.id}>
                        <span>{item.merchName} × {item.quantity}</span>
                        <span>{currencyFormatter.format(item.subtotal)}</span>
                      </div>
                    ))}
                  </div>
                )}

                <div className="mt-3 flex flex-wrap items-center justify-between gap-3 border-t border-white/30 pt-3">
                  <p className="font-fredoka font-bold text-black-blue">
                    {currencyFormatter.format(order.totalAmount)}
                  </p>
                  {nextStatuses.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                      {nextStatuses.map(nextStatus => (
                        <button
                          className={[
                            "rounded-full border px-4 py-1.5 text-xs font-bold transition disabled:opacity-50",
                            nextStatus === "CANCELLED"
                              ? "border-peach/60 bg-peach/20 text-black-blue hover:bg-peach/40"
                              : "border-aqua/60 bg-aqua/20 text-black-blue hover:bg-aqua/40",
                          ].join(" ")}
                          disabled={updatingId === order.id}
                          key={nextStatus}
                          onClick={() => handleStatusUpdate(order.id, nextStatus)}
                          type="button"
                        >
                          {ORDER_NEXT_LABELS[nextStatus] ?? nextStatus}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ─── Profile Image Upload helper ───────────────────────────────────────────────

function ProfileImageUpload({ label, orgId, url, variant, onUrlChange }: {
  label: string; orgId: string; url: string;
  variant: "logo" | "cover"; onUrlChange: (url: string) => void;
}) {
  const [uploading, setUploading] = useState(false);
  const handle = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setUploading(true);
    try {
      const publicUrl = await uploadOrganizerImage(file, orgId, variant);
      onUrlChange(publicUrl);
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Không thể tải ảnh lên."));
    } finally {
      setUploading(false);
    }
  };
  return (
    <div>
      <label className="block text-xs font-semibold text-ink/70 mb-2">{label}</label>
      {url && (
        <div className="relative mb-2 inline-block group">
          <img alt={label} className={variant === "cover" ? "h-16 w-full max-w-xs rounded-xl object-cover border border-white/60" : "size-14 rounded-full object-cover border border-white/60"} src={url} />
          <button className="absolute -top-1.5 -right-1.5 size-5 rounded-full bg-peach border border-white text-xs font-bold text-black-blue hidden group-hover:flex items-center justify-center" onClick={() => onUrlChange("")} type="button">✕</button>
        </div>
      )}
      <label className={["inline-flex cursor-pointer items-center gap-2 rounded-full border border-white/60 bg-white/50 px-4 py-2 text-xs font-semibold text-black-blue hover:bg-white/80 transition", uploading ? "opacity-50 pointer-events-none" : ""].join(" ")}>
        {uploading ? "⏳ Đang tải..." : `📷 ${url ? "Đổi" : "Tải lên"} ${label.toLowerCase()}`}
        <input accept="image/*" className="hidden" disabled={uploading} onChange={handle} type="file" />
      </label>
    </div>
  );
}

// ─── Profile Tab ───────────────────────────────────────────────────────────────

function ProfileTab({ org, onUpdated }: { org: OrganizationResponse; onUpdated: (updated: OrganizationResponse) => void }) {
  const [form, setForm] = useState({
    name: org.name,
    description: org.description ?? "",
    logoUrl: org.logoUrl ?? "",
    coverUrl: org.coverUrl ?? "",
  });
  const [isSaving, setIsSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    setForm({
      name: org.name,
      description: org.description ?? "",
      logoUrl: org.logoUrl ?? "",
      coverUrl: org.coverUrl ?? "",
    });
    setIsEditing(false);
  }, [org.id]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) return;
    setIsSaving(true);
    try {
      const res = await updateOrganization(org.id, {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        logoUrl: form.logoUrl.trim() || undefined,
        coverUrl: form.coverUrl.trim() || undefined,
      });
      onUpdated(res.data);
      setIsEditing(false);
      toast.success("Đã cập nhật hồ sơ tổ chức.");
    } catch (err) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setIsSaving(false);
    }
  };

  const cancel = () => {
    setForm({
      name: org.name,
      description: org.description ?? "",
      logoUrl: org.logoUrl ?? "",
      coverUrl: org.coverUrl ?? "",
    });
    setIsEditing(false);
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="font-fredoka text-2xl font-bold text-black-blue">Hồ sơ tổ chức</h2>
        {!isEditing && (
          <button
            className="rounded-full border border-black-blue bg-white/50 px-4 py-1.5 text-xs font-bold text-black-blue transition hover:bg-black-blue hover:text-white"
            onClick={() => setIsEditing(true)}
            type="button"
          >
            Chỉnh sửa
          </button>
        )}
      </div>

      <div className="rounded-panel border border-white/50 bg-white/45 p-6 shadow-glass backdrop-blur-xl">
        {/* Logo preview */}
        <div className="mb-5 flex items-center gap-4">
          <div className="size-16 shrink-0 overflow-hidden rounded-full border-2 border-white/60 bg-white/30 shadow-glass flex items-center justify-center">
            {(isEditing ? form.logoUrl : org.logoUrl) ? (
              <img
                alt="logo"
                className="size-full object-cover"
                onError={e => { (e.target as HTMLImageElement).src = "https://placehold.co/64x64/e9feff/1a3a4a?text=ORG"; }}
                src={isEditing ? form.logoUrl : (org.logoUrl ?? "")}
              />
            ) : (
              <span className="font-fredoka text-xl font-bold text-black-blue/50">
                {org.name.slice(0, 2).toUpperCase()}
              </span>
            )}
          </div>
          <div>
            <p className="font-fredoka text-xl font-bold text-black-blue">{org.name}</p>
            <p className="text-xs text-ink/50">
              {STATUS_LABELS[org.status] ?? org.status} · Tạo {formatDate(org.createdAt)}
            </p>
          </div>
        </div>

        {isEditing ? (
          <form className="space-y-4" onSubmit={handleSave}>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Tên tổ chức *</label>
              <input
                className="w-full rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                required
                value={form.name}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-ink/70 mb-1">Mô tả</label>
              <textarea
                className="h-20 w-full resize-none rounded-xl border border-white/70 bg-white/50 px-3 py-2 text-sm text-black-blue focus:outline-aqua"
                onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                value={form.description}
              />
            </div>
            <ProfileImageUpload
              label="Logo tổ chức"
              orgId={org.id}
              url={form.logoUrl}
              variant="logo"
              onUrlChange={url => setForm(f => ({ ...f, logoUrl: url }))}
            />
            <ProfileImageUpload
              label="Ảnh bìa"
              orgId={org.id}
              url={form.coverUrl}
              variant="cover"
              onUrlChange={url => setForm(f => ({ ...f, coverUrl: url }))}
            />
            <div className="flex gap-2 pt-1">
              <button
                className="rounded-full bg-black-blue px-5 py-2 text-sm font-bold text-white transition hover:bg-ink disabled:opacity-50"
                disabled={isSaving}
                type="submit"
              >
                {isSaving ? "Đang lưu..." : "Lưu"}
              </button>
              <button
                className="rounded-full border border-ink/20 bg-white/50 px-5 py-2 text-sm font-semibold text-black-blue transition hover:bg-white"
                onClick={cancel}
                type="button"
              >
                Huỷ
              </button>
            </div>
          </form>
        ) : (
          <div className="space-y-3 text-sm">
            {org.description ? (
              <p className="text-ink/70 leading-6">{org.description}</p>
            ) : (
              <p className="text-ink/50 italic">Chưa có mô tả. Bấm "Chỉnh sửa" để cập nhật.</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Main Dashboard Page ───────────────────────────────────────────────────────

export function OrganizerDashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [orgs, setOrgs] = useState<OrganizationResponse[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(null);
  const [isLoadingOrgs, setIsLoadingOrgs] = useState(true);
  const [activeTab, setActiveTab] = useState<Tab>("merch");
  const [showCreateOrg, setShowCreateOrg] = useState(false);

  useEffect(() => {
    if (!user || user.role !== "ORGANIZER") return;

    setIsLoadingOrgs(true);
    getOwnOrganizations()
      .then(res => {
        const list = res.data ?? [];
        setOrgs(list);
        if (list.length > 0) setSelectedOrgId(list[0].id);
      })
      .catch(() => toast.error("Không thể tải danh sách tổ chức."))
      .finally(() => setIsLoadingOrgs(false));
  }, [user]);

  if (!user) {
    return <Navigate replace state={{ from: "/organizer" }} to="/auth" />;
  }

  if (user.role !== "ORGANIZER") {
    return <Navigate replace to="/" />;
  }

  const selectedOrg = orgs.find(o => o.id === selectedOrgId);

  const TABS: { id: Tab; label: string }[] = [
    { id: "merch", label: "Vật phẩm" },
    { id: "events", label: "Sự kiện" },
    { id: "orders", label: "Đơn hàng" },
    { id: "profile", label: "Hồ sơ" },
  ];

  return (
    <main className="relative min-h-screen bg-transparent px-5 pb-16 pt-28 sm:px-8 lg:px-16">
      <Suspense fallback={<div className="fixed inset-0 bg-[#E9FEFF]" />}>
        <ShaderBackground />
      </Suspense>

      <div className="relative z-10 mx-auto max-w-5xl">
        <div className="mb-6">
          <p className="font-sans text-xs uppercase tracking-widest text-ink/50">
            Organizer Space
          </p>
          <h1 className="mt-1 font-fredoka text-4xl font-bold text-black-blue">
            Dashboard
          </h1>
        </div>

        {isLoadingOrgs ? (
          <div className="py-20 text-center text-sm text-ink/60">
            Đang tải thông tin tổ chức...
          </div>
        ) : (
          <>
            {/* Org Selector */}
            <div className="mb-6 rounded-panel border border-white/50 bg-white/45 p-5 shadow-glass backdrop-blur-xl">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex flex-wrap items-center gap-3">
                  <p className="text-sm font-semibold text-ink/60">Tổ chức:</p>
                  {orgs.length > 0 ? (
                    <select
                      className="rounded-xl border border-white/70 bg-white/60 px-3 py-1.5 text-sm font-semibold text-black-blue focus:outline-aqua"
                      onChange={e => {
                        setSelectedOrgId(e.target.value);
                        setActiveTab("merch");
                      }}
                      value={selectedOrgId ?? ""}
                    >
                      {orgs.map(org => (
                        <option key={org.id} value={org.id}>
                          {org.name}
                          {org.status !== "ACTIVE" ? ` (${STATUS_LABELS[org.status] ?? org.status})` : ""}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <p className="text-sm text-ink/60">Chưa có tổ chức nào.</p>
                  )}
                  {selectedOrg && (
                    <span className="rounded-full bg-white/50 border border-white/60 px-2.5 py-0.5 text-xs font-semibold text-black-blue">
                      {STATUS_LABELS[selectedOrg.status] ?? selectedOrg.status}
                    </span>
                  )}
                </div>
                <button
                  className="rounded-full border border-black-blue bg-white/50 px-4 py-1.5 text-xs font-bold text-black-blue transition hover:bg-black-blue hover:text-white"
                  onClick={() => setShowCreateOrg(v => !v)}
                  type="button"
                >
                  {showCreateOrg ? "Đóng" : "+ Tạo tổ chức"}
                </button>
              </div>

              {showCreateOrg && (
                <div className="mt-5 border-t border-white/40 pt-5">
                  <CreateOrgForm
                    onCreated={org => {
                      setOrgs(prev => [...prev, org]);
                      setSelectedOrgId(org.id);
                      setShowCreateOrg(false);
                    }}
                  />
                </div>
              )}
            </div>

            {selectedOrgId && selectedOrg ? (
              <>
                {/* Tab Bar */}
                <div className="mb-5 flex gap-1 rounded-full border border-white/50 bg-white/40 p-1 w-fit shadow-glass backdrop-blur-xl">
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

                {/* Tab Content */}
                {activeTab === "merch" && <MerchTab orgId={selectedOrgId} />}
                {activeTab === "events" && <EventsTab orgId={selectedOrgId} />}
                {activeTab === "orders" && <OrgOrdersTab orgId={selectedOrgId} />}
                {activeTab === "profile" && (
                  <ProfileTab
                    key={selectedOrg.id}
                    org={selectedOrg}
                    onUpdated={updated => setOrgs(prev => prev.map(o => o.id === updated.id ? updated : o))}
                  />
                )}
              </>
            ) : (
              <div className="py-16 text-center rounded-panel border border-white/40 bg-white/30 shadow-glass backdrop-blur-xl">
                <p className="font-fredoka text-xl font-bold text-black-blue">
                  Tạo tổ chức để bắt đầu
                </p>
                <p className="mt-2 text-sm text-ink/60">
                  Bấm "+ Tạo tổ chức" ở trên để đăng ký tổ chức của bạn.
                </p>
              </div>
            )}
          </>
        )}
      </div>
    </main>
  );
}
