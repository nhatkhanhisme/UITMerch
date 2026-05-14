import {
  useEffect,
  useMemo,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { Button, Input } from "../components/ui";
import { getApiErrorMessage } from "../api/auth";
import { uploadAvatarImage } from "../api/storage";
import { getCustomerProfile, updateCustomerProfile } from "../api/profile";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { CustomerProfile } from "../types/profile";
import {
  formatDate,
  getInitials,
  ProfileInfoRow,
  toOptionalValue,
} from "./profileUtils";

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
    (isAvatarRemoved || customerForm.avatarUrl !== (customerProfile?.avatarUrl ?? ""));

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
      toast.success("Avatar uploaded. Save changes to apply it.");
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
    toast.info("Avatar removed. Save changes to apply it.");
  };

  const handleCustomerSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isUploadingAvatar) {
      toast.error("Please wait for the avatar upload to finish.");
      return;
    }

    const fullName = customerForm.fullName.trim();
    if (!fullName) {
      toast.error("Full name is required.");
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
        throw new Error("Profile update failed.");
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
      toast.success(response.message || "Profile updated.");
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

  return (
    <main className="relative min-h-screen bg-canvas pb-16 pt-28 sm:pt-32">
      <AmbientBackgroundGradients />

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-6 px-6">
        <header className="overflow-hidden rounded-[36px] border border-white/60 bg-white/75 shadow-[0_24px_80px_rgba(16,24,40,0.12)] backdrop-blur">
          <div className="grid gap-0 lg:grid-cols-[1.05fr_0.95fr]">
            <div className="bg-gradient-to-br from-white via-white/90 to-aqua/20 p-6 sm:p-8 lg:p-9">
              <div className="flex flex-wrap items-center gap-4">
                <div className="flex size-20 items-center justify-center overflow-hidden rounded-full bg-white shadow-[0_12px_30px_rgba(82,128,145,0.2)]">
                  {user.avatarUrl ? (
                    <img
                      alt={user.fullName}
                      className="size-full object-cover"
                      src={user.avatarUrl}
                    />
                  ) : (
                    <span className="font-brand text-2xl font-black text-black-blue">
                      {avatarLabel}
                    </span>
                  )}
                </div>
                <div className="min-w-0">
                  <div className="inline-flex items-center rounded-full border border-aqua/60 bg-white/80 px-3 py-1 font-sans text-[11px] font-semibold uppercase tracking-[0.28em] text-slate/70">
                    Customer profile
                  </div>
                  <h1 className="mt-3 font-brand text-3xl font-black text-black-blue sm:text-4xl">
                    {user.fullName}
                  </h1>
                  <p className="mt-2 max-w-xl font-google text-sm leading-6 text-gray">
                    Keep your delivery information and avatar up to date in a
                    focused, clean workspace.
                  </p>
                </div>
              </div>

              <div className="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                <ProfileInfoRow
                  label="Email"
                  value={customerProfile?.email ?? user.email}
                  description="Used for login and order updates."
                  leading="@"
                />
                <ProfileInfoRow
                  label="Member since"
                  value={formatDate(customerProfile?.createdAt)}
                  description="Your account timeline."
                  leading="•"
                />
                <ProfileInfoRow
                  label="Verification"
                  value={user.isVerified ? "Verified" : "Pending"}
                  description="Email verification status."
                  leading="✓"
                />
              </div>
            </div>

            <div className="p-6 sm:p-8 lg:p-9">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
                    Account actions
                  </p>
                  <h2 className="mt-2 font-brand text-2xl font-black text-black-blue">
                    {isEditing ? "Edit mode" : "Overview"}
                  </h2>
                </div>
                <Button
                  className="shrink-0"
                  disabled={isSaving}
                  type="button"
                  variant="outline"
                  onClick={handleLogout}
                >
                  Log out
                </Button>
              </div>

              {canEdit ? (
                <div className="mt-6 flex flex-wrap items-center gap-3">
                  {isEditing ? (
                    <>
                      {hasUnsavedAvatarChange ? (
                        <div className="inline-flex items-center rounded-full border border-gold/60 bg-gold/20 px-3 py-1.5 font-sans text-xs font-semibold text-black-blue shadow-glass">
                          ● Unsaved image change
                        </div>
                      ) : null}
                      <Button
                        type="button"
                        variant="outline"
                        onClick={cancelEditing}
                        disabled={isSaving || isUploadingAvatar}
                      >
                        Cancel
                      </Button>
                      <Button
                        form="customer-profile-form"
                        loading={isSaving}
                        type="submit"
                        disabled={isSaving || isUploadingAvatar}
                      >
                        Save changes
                      </Button>
                    </>
                  ) : (
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={startEditing}
                    >
                      Edit profile
                    </Button>
                  )}
                </div>
              ) : null}

              {!isEditing ? (
                <div className="mt-6 grid gap-3">
                  <ProfileInfoRow
                    label="Phone"
                    value={customerProfile?.phone ?? ""}
                    description="Useful for shipping contact."
                    leading="P"
                  />
                  <ProfileInfoRow
                    label="Address"
                    value={customerProfile?.address ?? ""}
                    description="Saved delivery destination."
                    leading="A"
                  />
                  <ProfileInfoRow
                    label="Role"
                    value="Customer"
                    description="Shop, save items, and track orders."
                    leading="C"
                  />
                </div>
              ) : null}
            </div>
          </div>
        </header>

        {isLoading ? (
          <div className="rounded-panel border border-white/60 bg-white/60 p-6 text-center font-sans text-sm text-gray shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            Loading your profile...
          </div>
        ) : null}



        {!isLoading && customerProfile && isEditing ? (
          <form
            className="grid gap-4 rounded-[32px] border border-white/60 bg-white/75 p-6 shadow-[0_20px_60px_rgba(16,24,40,0.12)] backdrop-blur-sm md:grid-cols-2"
            id="customer-profile-form"
            onSubmit={handleCustomerSave}
          >
            <Input
              label="Full name"
              name="fullName"
              onChange={handleCustomerChange}
              placeholder="Your full name"
              required
              value={customerForm.fullName}
            />
            <Input
              label="Email"
              name="email"
              placeholder="you@uit.edu.vn"
              value={customerProfile.email}
              disabled
            />
            <Input
              label="Phone"
              name="phone"
              onChange={handleCustomerChange}
              placeholder="Optional"
              value={customerForm.phone}
            />
            <Input
              label="Address"
              name="address"
              onChange={handleCustomerChange}
              placeholder="Optional"
              value={customerForm.address}
            />
            <div className="md:col-span-2">
              <label
                className="font-sans text-sm text-gray"
                htmlFor="avatar-upload"
              >
                Avatar image
              </label>
              <div className="mt-2 flex flex-wrap items-center gap-4">
                <div className="relative flex size-16 items-center justify-center overflow-hidden rounded-full bg-white shadow-[0_12px_30px_rgba(82,128,145,0.2)]">
                  {customerForm.avatarUrl ? (
                    <img
                      alt={customerForm.fullName}
                      className="size-full object-cover"
                      src={customerForm.avatarUrl}
                    />
                  ) : (
                    <span className="font-brand text-lg font-black text-black-blue">
                      {avatarLabel}
                    </span>
                  )}
                  {customerForm.avatarUrl ? (
                    <button
                      aria-label="Remove avatar"
                      className="absolute right-0 top-0 flex size-6 -translate-y-1/3 translate-x-1/3 items-center justify-center rounded-full border border-white/70 bg-peach text-xs font-bold text-black-blue shadow-glass"
                      disabled={isSaving || isUploadingAvatar}
                      onClick={handleAvatarRemove}
                      type="button"
                    >
                      x
                    </button>
                  ) : null}
                </div>
                <div className="flex-1">
                  <input
                    accept="image/*"
                    className="h-12 w-full rounded-glass border border-white/40 bg-white/20 px-4 font-sans text-sm text-ink shadow-glass transition duration-200 file:mr-4 file:rounded-glass file:border-0 file:bg-aqua file:px-4 file:py-2 file:font-sans file:text-sm file:font-semibold file:text-black-blue hover:file:bg-gold"
                    disabled={isSaving || isUploadingAvatar}
                    id="avatar-upload"
                    onChange={handleAvatarUpload}
                    type="file"
                  />
                  <p className="mt-2 font-sans text-xs text-gray">
                    {isUploadingAvatar
                      ? "Uploading avatar..."
                      : "PNG, JPG, GIF, SVG, or WEBP up to 10MB."}
                  </p>
                </div>
              </div>
            </div>
          </form>
        ) : null}
      </div>
    </main>
  );
}
