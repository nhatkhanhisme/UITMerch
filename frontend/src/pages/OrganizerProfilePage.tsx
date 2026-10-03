import {
  useEffect,
  useMemo,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { Button, Input } from "../components/ui";
import { getApiErrorMessage } from "../api/auth";
import { uploadOrganizerImage } from "../api/storage";
import { getOwnOrganizations } from "../api/organization";
import { updateOrganizerProfile } from "../api/profile";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { OrganizerProfile } from "../types/profile";
import {
  getInitials,
  ProfileInfoRow,
  toOptionalValue,
} from "./profileUtils";

// ─── Icons ────────────────────────────────────────────────────────────────────
function CalendarIcon() {
  return (
    <svg aria-hidden="true" className="size-4 shrink-0 text-gold" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
    </svg>
  );
}

function ShieldIcon() {
  return (
    <svg aria-hidden="true" className="size-4 shrink-0 text-aqua" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  );
}

function CameraIcon() {
  return (
    <svg aria-hidden="true" className="size-4 shrink-0 text-black-blue" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  );
}

function ImageIcon() {
  return (
    <svg aria-hidden="true" className="size-4 shrink-0 text-black-blue" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
    </svg>
  );
}

const initialOrganizerForm = {
  name: "",
  description: "",
  logoUrl: "",
  coverUrl: "",
};

export function OrganizerProfilePage() {
  const user = useAuthStore((state) => state.user);
  const updateUser = useAuthStore((state) => state.updateUser);
  const clearSession = useAuthStore((state) => state.clearSession);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isUploadingLogo, setIsUploadingLogo] = useState(false);
  const [isUploadingCover, setIsUploadingCover] = useState(false);
  const [isLogoRemoved, setIsLogoRemoved] = useState(false);
  const [isCoverRemoved, setIsCoverRemoved] = useState(false);
  const [isMissingOrganization, setIsMissingOrganization] = useState(false);
  const [organizations, setOrganizations] = useState<OrganizerProfile[]>([]);
  const [organizerProfile, setOrganizerProfile] =
    useState<OrganizerProfile | null>(null);
  const [organizerForm, setOrganizerForm] = useState(initialOrganizerForm);
  const location = useLocation();
  const navigate = useNavigate();
  const returnTo = `${location.pathname}${location.search}${location.hash}`;
  const userId = user?.id;
  const isUploadingMedia = isUploadingLogo || isUploadingCover;

  const avatarLabel = useMemo(() => {
    if (!user?.fullName) {
      return "O";
    }
    return getInitials(user.fullName);
  }, [user?.fullName]);

  useEffect(() => {
    if (!userId || user?.role !== "ORGANIZER") {
      return;
    }

    let isActive = true;

    const loadProfile = async () => {
      setIsLoading(true);
      setIsMissingOrganization(false);

      try {
        const response = await getOwnOrganizations({ size: 100 });
        const rows: OrganizerProfile[] = (response.data ?? []).map((row) => ({ ...row, status: row.status as OrganizerProfile["status"], createdAt: row.createdAt ?? "", updatedAt: row.updatedAt ?? "" }));
        if (!rows.length) { setIsMissingOrganization(true); return; }
        setOrganizations(rows);
        const profile = rows[0];

        if (!profile || !isActive) {
          return;
        }

        setOrganizerProfile(profile);
        setOrganizerForm({
          name: profile.name,
          description: profile.description ?? "",
          logoUrl: profile.logoUrl ?? "",
          coverUrl: profile.coverUrl ?? "",
        });
        updateUser({ avatarUrl: profile.logoUrl ?? null });
      } catch (error) {
        if (!isActive) return;
        const msg = getApiErrorMessage(error);
        if (msg === "Chưa có tổ chức nào.") {
          setIsMissingOrganization(true);
        } else {
          toast.error(msg);
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    };

    void loadProfile();

    return () => {
      isActive = false;
    };
  }, [updateUser, user?.role, userId]);

  if (!user) {
    return <Navigate replace state={{ from: returnTo }} to="/auth" />;
  }

  if (user.role !== "ORGANIZER") {
    return (
      <Navigate
        replace
        to={user.role === "CUSTOMER" ? "/profile/customer" : "/"}
      />
    );
  }

  const canEdit =
    !isLoading && Boolean(organizerProfile) && !isMissingOrganization;

  const hasUnsavedMediaChange =
    isEditing &&
    (isLogoRemoved ||
      isCoverRemoved ||
      organizerForm.logoUrl !== (organizerProfile?.logoUrl ?? "") ||
      organizerForm.coverUrl !== (organizerProfile?.coverUrl ?? ""));

  const startEditing = () => {
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setIsUploadingLogo(false);
    setIsUploadingCover(false);
    setIsLogoRemoved(false);
    setIsCoverRemoved(false);

    if (organizerProfile) {
      setOrganizerForm({
        name: organizerProfile.name,
        description: organizerProfile.description ?? "",
        logoUrl: organizerProfile.logoUrl ?? "",
        coverUrl: organizerProfile.coverUrl ?? "",
      });
    }
  };

  const handleOrganizerChange = (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = event.target;
    setOrganizerForm((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const handleOrganizerLogoUpload = async (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file || !organizerProfile) {
      return;
    }

    setIsUploadingLogo(true);
    const input = event.target;

    try {
      const publicUrl = await uploadOrganizerImage(
        file,
        organizerProfile.id,
        "logo",
      );
      setOrganizerForm((current) => ({
        ...current,
        logoUrl: publicUrl,
      }));
      setIsLogoRemoved(false);
      toast.success("Đã tải logo. Lưu thay đổi để áp dụng.");
    } catch (error) {
      toast.error(getApiErrorMessage(error));
    } finally {
      setIsUploadingLogo(false);
      input.value = "";
    }
  };

  const handleOrganizerLogoRemove = () => {
    setOrganizerForm((current) => ({
      ...current,
      logoUrl: "",
    }));
    setIsLogoRemoved(true);
    toast.info("Đã bỏ logo. Lưu thay đổi để áp dụng.");
  };

  const handleOrganizerCoverUpload = async (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file || !organizerProfile) {
      return;
    }

    setIsUploadingCover(true);
    const input = event.target;

    try {
      const publicUrl = await uploadOrganizerImage(
        file,
        organizerProfile.id,
        "cover",
      );
      setOrganizerForm((current) => ({
        ...current,
        coverUrl: publicUrl,
      }));
      setIsCoverRemoved(false);
      toast.success("Đã tải ảnh bìa. Lưu thay đổi để áp dụng.");
    } catch (error) {
      toast.error(getApiErrorMessage(error));
    } finally {
      setIsUploadingCover(false);
      input.value = "";
    }
  };

  const handleOrganizerCoverRemove = () => {
    setOrganizerForm((current) => ({
      ...current,
      coverUrl: "",
    }));
    setIsCoverRemoved(true);
    toast.info("Đã bỏ ảnh bìa. Lưu thay đổi để áp dụng.");
  };

  const handleOrganizerSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isUploadingMedia) {
      toast.error("Vui lòng chờ ảnh tải lên hoàn tất.");
      return;
    }

    const name = organizerForm.name.trim();
    if (!name) {
      toast.error("Vui lòng nhập tên tổ chức.");
      return;
    }

    setIsSaving(true);

    try {
      if (!organizerProfile?.id) throw new Error("Không tìm thấy tổ chức.");
      const response = await updateOrganizerProfile(organizerProfile.id, {
        name,
        description: organizerForm.description.trim(),
        logoUrl: isLogoRemoved ? "" : toOptionalValue(organizerForm.logoUrl),
        coverUrl: isCoverRemoved ? "" : toOptionalValue(organizerForm.coverUrl),
      });
      const profile = response.data;

      if (!profile) {
        throw new Error("Không thể cập nhật hồ sơ tổ chức.");
      }

      setOrganizerProfile(profile);
      setOrganizerForm({
        name: profile.name,
        description: profile.description ?? "",
        logoUrl: profile.logoUrl ?? "",
        coverUrl: profile.coverUrl ?? "",
      });
      setIsLogoRemoved(false);
      setIsCoverRemoved(false);
      updateUser({ avatarUrl: profile.logoUrl ?? null });
      toast.success("Đã cập nhật hồ sơ tổ chức.");
      setOrganizations((rows) => rows.map((row) => row.id === profile.id ? profile : row));
      setIsEditing(false);
    } catch (error) {
      if (
        error &&
        typeof error === "object" &&
        "response" in error &&
        // keep 404 handling explicit for missing organization profiles
        (error as { response?: { status?: number } }).response?.status === 404
      ) {
        setIsMissingOrganization(true);
        setOrganizerProfile(null);
        setIsEditing(false);
      } else {
        toast.error(getApiErrorMessage(error));
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleLogout = () => {
    clearSession();
    navigate("/");
  };

  // Resolve which logo/cover to show in the left hero panel
  const displayedLogoUrl = isEditing
    ? organizerForm.logoUrl
    : (organizerProfile?.logoUrl ?? user.avatarUrl ?? "");
  const displayedCoverUrl = isEditing
    ? organizerForm.coverUrl
    : (organizerProfile?.coverUrl ?? "");

  const orgStatus = ({ ACTIVE: "Đang hoạt động", PENDING: "Chờ duyệt", INACTIVE: "Tạm ngừng" } as Record<string, string>)[organizerProfile?.status ?? ""] ?? "Chưa cập nhật";
  const dateLabel = (value?: string | null) => value ? new Date(value).toLocaleDateString("vi-VN") : "Chưa cập nhật";
  return (
    <main className="relative min-h-screen bg-canvas pb-16 pt-28 sm:pt-32 organizer-profile-page">
      <AmbientBackgroundGradients />
      <div className="relative z-10 mx-auto max-w-6xl space-y-6 px-4 sm:px-6">
        {organizations.length > 1 && <label className="organization-selector">Tổ chức đang chỉnh sửa<select value={organizerProfile?.id ?? ""} disabled={isSaving || isUploadingMedia} onChange={(e) => {
          const selected = organizations.find((org) => org.id === e.target.value);
          if (!selected) return;
          setOrganizerProfile(selected);
          setOrganizerForm({ name: selected.name, description: selected.description ?? "", logoUrl: selected.logoUrl ?? "", coverUrl: selected.coverUrl ?? "" });
          setIsEditing(false);setIsLogoRemoved(false);setIsCoverRemoved(false);
        }}>{organizations.map((org) => <option value={org.id} key={org.id}>{org.name}</option>)}</select></label>}
        {isLoading ? <section className="order-card" role="status">Đang tải hồ sơ tổ chức…</section> : isMissingOrganization ? <section className="order-card"><h1>Chưa có hồ sơ tổ chức</h1><p className="panel-description">Tạo tổ chức để quản lý vật phẩm, sự kiện và đơn hàng.</p><Link className="feature-button mt-4 inline-flex" to="/organizer">Tạo tổ chức</Link></section> : (
          <form className="customer-profile-card" id="organizer-profile-form" onSubmit={handleOrganizerSave}>
            <div className="customer-profile-grid">
              <section className="customer-profile-identity organizer-profile-identity">
                {displayedCoverUrl && <img className="organizer-profile-cover" src={displayedCoverUrl} alt="Ảnh bìa tổ chức" />}
                <div className="flex items-start gap-4">
                  <div className="profile-avatar">{displayedLogoUrl ? <img src={displayedLogoUrl} alt="Logo tổ chức" /> : <span>{organizerForm.name.slice(0, 2).toUpperCase() || avatarLabel}</span>}</div>
                  <div className="min-w-0"><p className="order-eyebrow">Hồ sơ tổ chức</p><h1 className="mt-3 break-words text-2xl font-bold sm:text-3xl">{organizerProfile?.name ?? user.fullName}</h1></div>
                </div>
                <div className="mt-5 space-y-3 text-sm text-slate"><p className="flex items-center gap-2"><ShieldIcon />{orgStatus}</p><p className="flex items-center gap-2"><CalendarIcon />Tham gia từ {dateLabel(organizerProfile?.createdAt)}</p></div>
                <nav className="profile-shortcuts" aria-label="Tiện ích tổ chức"><Link to={`/organizer?orgId=${organizerProfile?.id ?? ""}`}>Quản lý tổ chức</Link><Link to={`/organization/${organizerProfile?.id ?? ""}`}>Xem trang tổ chức</Link></nav>
              </section>
              <section className="customer-profile-details">
                <div className="flex flex-wrap items-center justify-between gap-4"><div><p className="order-eyebrow">Thông tin tổ chức</p><h2 className="mt-2 text-2xl font-bold">{isEditing ? "Chỉnh sửa hồ sơ" : "Giới thiệu tổ chức"}</h2></div><Button disabled={isSaving || isUploadingMedia} variant="outline" type="button" onClick={handleLogout}>Đăng xuất</Button></div>
                {!isEditing && canEdit && <Button className="mt-6" type="button" variant="secondary" onClick={startEditing}>Chỉnh sửa hồ sơ</Button>}
                <div className="mt-6">{isEditing ? <div className="organizer-profile-fields space-y-5">
                  <Input label="Tên tổ chức" name="name" required maxLength={200} value={organizerForm.name} onChange={handleOrganizerChange} disabled={isSaving || isUploadingMedia} />
                  <label className="edit-field">Giới thiệu tổ chức<textarea name="description" maxLength={2000} rows={5} value={organizerForm.description} onChange={handleOrganizerChange} disabled={isSaving || isUploadingMedia} placeholder="Giới thiệu hoạt động và cộng đồng của tổ chức…" /></label>
                  <div className="profile-media-fields">
                    <section><h3>Logo tổ chức</h3><p>Ảnh đại diện trên trang tổ chức.</p><label className="media-upload-label" htmlFor="org-logo-upload"><CameraIcon />{isUploadingLogo ? "Đang tải…" : "Chọn logo"}</label><input id="org-logo-upload" type="file" accept="image/*" className="sr-only" onChange={handleOrganizerLogoUpload} disabled={isSaving || isUploadingMedia} />{organizerForm.logoUrl && <button type="button" disabled={isSaving || isUploadingMedia} onClick={handleOrganizerLogoRemove}>Bỏ logo</button>}</section>
                    <section><h3>Ảnh bìa</h3><p>Ảnh giới thiệu tổ chức.</p><label className="media-upload-label" htmlFor="org-cover-upload"><ImageIcon />{isUploadingCover ? "Đang tải…" : "Chọn ảnh bìa"}</label><input id="org-cover-upload" type="file" accept="image/*" className="sr-only" onChange={handleOrganizerCoverUpload} disabled={isSaving || isUploadingMedia} />{organizerForm.coverUrl && <button type="button" disabled={isSaving || isUploadingMedia} onClick={handleOrganizerCoverRemove}>Bỏ ảnh bìa</button>}</section>
                  </div>
                  {hasUnsavedMediaChange && <p className="panel-description" role="status">Ảnh đã thay đổi. Lưu để áp dụng.</p>}
                  <div className="flex flex-wrap gap-3 border-t border-gray/20 pt-5"><Button type="submit" loading={isSaving} disabled={isSaving || isUploadingMedia}>Lưu thay đổi</Button><Button type="button" variant="outline" disabled={isSaving || isUploadingMedia} onClick={cancelEditing}>Hủy chỉnh sửa</Button></div>
                </div> : <div className="space-y-4"><ProfileInfoRow label="Giới thiệu" value={organizerProfile?.description} description="Thông tin công khai trên trang tổ chức." /><ProfileInfoRow label="Cập nhật gần nhất" value={dateLabel(organizerProfile?.updatedAt)} /></div>}</div>
              </section>
            </div>
          </form>
        )}
      </div>
    </main>
  );
}
