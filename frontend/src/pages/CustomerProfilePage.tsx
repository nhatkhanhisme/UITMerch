import {
  Phone,
  MapPin,
  Package,
  Building2,
  Bell,
  ArrowRight,
} from "lucide-react";
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
import { uploadAvatarImage } from "../api/storage";
import { getCustomerProfile, updateCustomerProfile } from "../api/profile";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { CustomerProfile } from "../types/profile";
import { getInitials, ProfileInfoRow, toOptionalValue } from "./profileUtils";

// ─── Custom Icons ─────────────────────────────────────────────────────────────
function MailIcon() {
  return (
    <svg aria-hidden="true" className="size-4 shrink-0 text-aqua" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
    </svg>
  );
}

function CalendarIcon() {
  return (
    <svg aria-hidden="true" className="size-4 shrink-0 text-gold" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
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

const initialCustomerForm = {
  fullName: "",
  phone: "",
  address: "",
  avatarUrl: "",
};

export function CustomerProfilePage() {
  const user = useAuthStore((state) => state.user);
  const updateUser = useAuthStore((state) => state.updateUser);
  const clearSession = useAuthStore((state) => state.clearSession);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [isAvatarRemoved, setIsAvatarRemoved] = useState(false);
  const [customerProfile, setCustomerProfile] =
    useState<CustomerProfile | null>(null);
  const [customerForm, setCustomerForm] = useState(initialCustomerForm);
  const location = useLocation();
  const navigate = useNavigate();
  const returnTo = `${location.pathname}${location.search}${location.hash}`;
  const userId = user?.id;

  const avatarLabel = useMemo(() => {
    if (!user?.fullName) {
      return "U";
    }

    return getInitials(user.fullName);
  }, [user?.fullName]);

  useEffect(() => {
    if (!userId || user?.role !== "CUSTOMER") {
      return;
    }

    let isActive = true;

    const loadProfile = async () => {
      setIsLoading(true);

      try {
        const response = await getCustomerProfile();
        const profile = response.data;

        if (!profile || !isActive) {
          return;
        }

        setCustomerProfile(profile);
        setCustomerForm({
          fullName: profile.fullName,
          phone: profile.phone ?? "",
          address: profile.address ?? "",
          avatarUrl: profile.avatarUrl ?? "",
        });
        updateUser({
          fullName: profile.fullName,
          avatarUrl: profile.avatarUrl ?? null,
        });
      } catch (error) {
        if (isActive) {
          toast.error(getApiErrorMessage(error));
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

  if (user.role !== "CUSTOMER") {
    return (
      <Navigate
        replace
        to={user.role === "ORGANIZER" ? "/profile/organizer" : "/"}
      />
    );
  }

  const canEdit = !isLoading && Boolean(customerProfile);

  const hasUnsavedAvatarChange =
    isEditing &&
    (isAvatarRemoved ||
      customerForm.avatarUrl !== (customerProfile?.avatarUrl ?? ""));

  const startEditing = () => {
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setIsUploadingAvatar(false);
    setIsAvatarRemoved(false);

    if (customerProfile) {
      setCustomerForm({
        fullName: customerProfile.fullName,
        phone: customerProfile.phone ?? "",
        address: customerProfile.address ?? "",
        avatarUrl: customerProfile.avatarUrl ?? "",
      });
    }
  };

  const handleCustomerChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setCustomerForm((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const handleAvatarUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !user) {
      return;
    }

    setIsUploadingAvatar(true);

    const input = event.target;

    try {
      const publicUrl = await uploadAvatarImage(file, user.id);
      setCustomerForm((current) => ({
        ...current,
        avatarUrl: publicUrl,
      }));
      setIsAvatarRemoved(false);
      toast.success("Ảnh đã tải lên. Lưu thay đổi để cập nhật hồ sơ.");
    } catch (error) {
      toast.error(getApiErrorMessage(error));
    } finally {
      setIsUploadingAvatar(false);
      input.value = "";
    }
  };

  const handleAvatarRemove = () => {
    setCustomerForm((current) => ({
      ...current,
      avatarUrl: "",
    }));
    setIsAvatarRemoved(true);
    toast.info("Ảnh đã được gỡ. Lưu thay đổi để cập nhật hồ sơ.");
  };

  const handleCustomerSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isUploadingAvatar) {
      toast.error("Vui lòng chờ ảnh tải lên hoàn tất.");
      return;
    }

    const fullName = customerForm.fullName.trim();
    if (!fullName) {
      toast.error("Vui lòng nhập họ và tên.");
      return;
    }

    setIsSaving(true);

    try {
      const response = await updateCustomerProfile({
        fullName,
        phone: toOptionalValue(customerForm.phone),
        address: toOptionalValue(customerForm.address),
        avatarUrl: isAvatarRemoved
          ? ""
          : toOptionalValue(customerForm.avatarUrl),
      });
      const profile = response.data;

      if (!profile) {
        throw new Error("Không thể cập nhật hồ sơ.");
      }

      setCustomerProfile(profile);
      setCustomerForm({
        fullName: profile.fullName,
        phone: profile.phone ?? "",
        address: profile.address ?? "",
        avatarUrl: profile.avatarUrl ?? "",
      });
      setIsAvatarRemoved(false);
      updateUser({
        fullName: profile.fullName,
        avatarUrl: profile.avatarUrl ?? null,
      });
      toast.success("Đã cập nhật hồ sơ.");
      setIsEditing(false);
    } catch (error) {
      toast.error(getApiErrorMessage(error));
    } finally {
      setIsSaving(false);
    }
  };

  const handleLogout = () => {
    clearSession();
    navigate("/");
  };

  const displayedAvatar = isEditing ? customerForm.avatarUrl : user.avatarUrl;

  return (
    <main className="relative min-h-screen bg-canvas pb-16 pt-28 sm:pt-32">
      <AmbientBackgroundGradients />

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 sm:px-6">
        <form
          className="customer-profile-card"
          id="customer-profile-form"
          onSubmit={handleCustomerSave}
        >
          <div className="grid gap-0 lg:grid-cols-[0.95fr_1.05fr]">
            {/* ── Left Identity & Minimal Info Panel ── */}
            <div className="customer-profile-identity">
              <div className="flex flex-wrap items-center gap-4">
                {/* Avatar Display & Inline Controls */}
                <div className="relative flex flex-col items-center gap-2">
                  <div className="relative inline-block">
                    <div className="relative flex size-20 shrink-0 items-center justify-center overflow-hidden rounded-full bg-white shadow-[0_12px_30px_rgba(82,128,145,0.2)]">
                      {displayedAvatar ? (
                        <img
                          alt={
                            isEditing ? customerForm.fullName : user.fullName
                          }
                          className="size-full object-cover"
                          src={displayedAvatar}
                        />
                      ) : (
                        <span className="font-brand text-2xl font-black text-black-blue">
                          {avatarLabel}
                        </span>
                      )}
                    </div>
                    {isEditing && customerForm.avatarUrl ? (
                      <button
                        aria-label="Gỡ ảnh đại diện"
                        className="absolute right-0 top-0 flex size-6 -translate-y-1/3 translate-x-1/3 items-center justify-center rounded-full border border-white/70 bg-peach text-xs font-bold text-black-blue shadow-glass z-10 hover:scale-110 transition"
                        disabled={isSaving || isUploadingAvatar}
                        onClick={handleAvatarRemove}
                        type="button"
                      >
                        ✕
                      </button>
                    ) : null}
                  </div>

                  {/* Inline Trigger below avatar in edit mode */}
                  {isEditing ? (
                    <div className="flex flex-col items-center gap-1">
                      <label
                        className="inline-flex cursor-pointer items-center gap-1.5 rounded-full border border-white/60 bg-white/80 px-3 py-1 font-sans text-xs font-semibold text-black-blue shadow-sm transition hover:bg-white"
                        htmlFor="inline-avatar-upload"
                      >
                        <CameraIcon />
                        <span>Đổi ảnh</span>
                      </label>
                      <input
                        accept="image/*"
                        className="hidden"
                        disabled={isSaving || isUploadingAvatar}
                        id="inline-avatar-upload"
                        onChange={handleAvatarUpload}
                        type="file"
                      />
                      {isUploadingAvatar ? (
                        <span className="text-[10px] text-gray animate-pulse">
                          Đang tải ảnh…
                        </span>
                      ) : null}
                    </div>
                  ) : null}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="inline-flex items-center rounded-full border border-aqua/60 bg-white/80 px-3 py-1 font-sans text-[11px] font-semibold uppercase tracking-[0.28em] text-slate">
                    Hồ sơ của tôi
                  </div>

                  {/* Dynamic Heading / Inline Name Input */}
                  {isEditing ? (
                    <div className="mt-3 max-w-sm">
                      <Input
                        label="Họ và tên"
                        name="fullName"
                        onChange={handleCustomerChange}
                        placeholder="Nhập họ và tên"
                        required
                        value={customerForm.fullName}
                        disabled={isSaving || isUploadingAvatar}
                      />
                    </div>
                  ) : (
                    <h1 className="mt-3 break-words font-brand text-2xl font-bold text-black-blue sm:text-3xl">
                      {user.fullName}
                    </h1>
                  )}

                  {/* Minimalist Fixed Metadata Rows */}
                  <div className="customer-profile-meta mt-4 flex flex-col gap-3 text-sm text-gray">
                    <div className="flex items-center gap-1.5">
                      <MailIcon />
                      <span className="break-all">
                        {customerProfile?.email ?? user.email}
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <CalendarIcon />
                      <span>
                        Tham gia từ{" "}
                        {customerProfile?.createdAt
                          ? new Date(
                              customerProfile.createdAt,
                            ).toLocaleDateString("vi-VN")
                          : "chưa cập nhật"}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              <nav
                className="profile-shortcuts"
                aria-label="Tiện ích tài khoản"
              >
                <Link to="/orders">
                  <Package aria-hidden="true" size={20} />
                  <span>Đơn hàng của tôi</span>
                  <ArrowRight aria-hidden="true" size={18} />
                </Link>
                <Link to="/following">
                  <Building2 aria-hidden="true" size={20} />
                  <span>Tổ chức đang theo dõi</span>
                  <ArrowRight aria-hidden="true" size={18} />
                </Link>
                <Link to="/restock-subscriptions">
                  <Bell aria-hidden="true" size={20} />
                  <span>Thông báo có hàng</span>
                  <ArrowRight aria-hidden="true" size={18} />
                </Link>
              </nav>
            </div>

            {/* ── Right Action Controls & Inline Fields Panel ── */}
            <div className="customer-profile-details">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate">
                    Thông tin tài khoản
                  </p>
                  <h2 className="mt-2 font-brand text-2xl font-black text-black-blue">
                    {isEditing ? "Chỉnh sửa hồ sơ" : "Thông tin liên hệ"}
                  </h2>
                </div>
                <Button
                  className="shrink-0"
                  disabled={isSaving}
                  type="button"
                  variant="outline"
                  onClick={handleLogout}
                >
                  Đăng xuất
                </Button>
              </div>

              {canEdit ? (
                <div className="mt-6 flex flex-wrap items-center gap-3">
                  {isEditing ? (
                    <>
                      {hasUnsavedAvatarChange ? (
                        <div className="inline-flex items-center rounded-full border border-gold/60 bg-gold/20 px-3 py-1.5 font-sans text-xs font-semibold text-black-blue shadow-glass">
                          ● Ảnh chưa được lưu
                        </div>
                      ) : null}
                      <Button
                        type="button"
                        variant="outline"
                        onClick={cancelEditing}
                        disabled={isSaving || isUploadingAvatar}
                      >
                        Hủy chỉnh sửa
                      </Button>
                      <Button
                        form="customer-profile-form"
                        loading={isSaving}
                        type="submit"
                        disabled={isSaving || isUploadingAvatar}
                      >
                        Lưu thay đổi
                      </Button>
                    </>
                  ) : (
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={startEditing}
                    >
                      Chỉnh sửa hồ sơ
                    </Button>
                  )}
                </div>
              ) : null}

              {/* Dynamic Property Presentation */}
              <div className="mt-6">
                {isEditing ? (
                  <div className="grid gap-4">
                    <Input
                      label="Số điện thoại"
                      name="phone"
                      onChange={handleCustomerChange}
                      placeholder="Chưa cập nhật"
                      value={customerForm.phone}
                      disabled={isSaving || isUploadingAvatar}
                    />
                    <Input
                      label="Địa chỉ"
                      name="address"
                      onChange={handleCustomerChange}
                      placeholder="Chưa cập nhật"
                      value={customerForm.address}
                      disabled={isSaving || isUploadingAvatar}
                    />
                  </div>
                ) : (
                  <div className="grid gap-3">
                    <ProfileInfoRow
                      label="Số điện thoại"
                      value={customerProfile?.phone ?? ""}
                      description="Dùng để liên hệ khi nhận hàng."
                      leading={<Phone size={18} />}
                    />
                    <ProfileInfoRow
                      label="Địa chỉ"
                      value={customerProfile?.address ?? ""}
                      description="Thông tin địa chỉ trong hồ sơ của bạn."
                      leading={<MapPin size={18} />}
                    />
                  </div>
                )}
              </div>
            </div>
          </div>
        </form>

        {isLoading ? (
          <div className="rounded-panel border border-white/60 bg-white/60 p-6 text-center font-sans text-sm text-gray shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            Đang tải hồ sơ…
          </div>
        ) : null}
      </div>
    </main>
  );
}
