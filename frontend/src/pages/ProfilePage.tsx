import {
  useEffect,
  useMemo,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import axios from "axios";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { Button, Input } from "../components/ui";
import { getApiErrorMessage } from "../api/auth";
import { uploadAvatarImage } from "../api/storage";
import {
  getCustomerProfile,
  getOrganizerProfile,
  updateCustomerProfile,
  updateOrganizerProfile,
} from "../api/profile";
import { useAuthStore } from "../stores/authStore";
import type { CustomerProfile, OrganizerProfile } from "../types/profile";

const formatDate = (value?: string | null) => {
  if (!value) {
    return "N/A";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "N/A";
  }

  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
};

const getInitials = (fullName: string) => {
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "U";
  }

  const [first, last] =
    parts.length === 1 ? [parts[0], ""] : [parts[0], parts[parts.length - 1]];
  return `${first[0] ?? ""}${last[0] ?? ""}`.toUpperCase();
};

const toOptionalValue = (value: string) => {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
};

const initialCustomerForm = {
  fullName: "",
  phone: "",
  address: "",
  avatarUrl: "",
};

const initialOrganizerForm = {
  name: "",
  description: "",
  logoUrl: "",
  coverUrl: "",
};

type InfoRowProps = {
  label: string;
  value?: string | null;
};

function InfoRow({ label, value }: InfoRowProps) {
  return (
    <div className="rounded-2xl border border-white/70 bg-white/70 px-4 py-3 shadow-[0_10px_24px_rgba(82,128,145,0.12)]">
      <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/60">
        {label}
      </p>
      <p className="mt-2 font-fredoka text-base font-bold text-black-blue">
        {value && value.trim().length > 0 ? value : "N/A"}
      </p>
    </div>
  );
}

export function ProfilePage() {
  const user = useAuthStore((state) => state.user);
  const updateUser = useAuthStore((state) => state.updateUser);
  const clearSession = useAuthStore((state) => state.clearSession);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isMissingOrganization, setIsMissingOrganization] = useState(false);
  const [customerProfile, setCustomerProfile] =
    useState<CustomerProfile | null>(null);
  const [organizerProfile, setOrganizerProfile] =
    useState<OrganizerProfile | null>(null);
  const [customerForm, setCustomerForm] = useState(initialCustomerForm);
  const [organizerForm, setOrganizerForm] = useState(initialOrganizerForm);
  const location = useLocation();
  const navigate = useNavigate();
  const returnTo = `${location.pathname}${location.search}${location.hash}`;
  const userId = user?.id;
  const userRole = user?.role;

  const avatarLabel = useMemo(() => {
    if (!user?.fullName) {
      return "U";
    }

    return getInitials(user.fullName);
  }, [user?.fullName]);

  useEffect(() => {
    if (!userId || !userRole) {
      return;
    }

    let isActive = true;

    const loadProfile = async () => {
      setIsLoading(true);
      setErrorMessage(null);
      setSuccessMessage(null);
      setIsMissingOrganization(false);

      try {
        if (userRole === "CUSTOMER") {
          const response = await getCustomerProfile();
          const profile = response.data;

          if (!profile) {
            throw new Error("Profile not available.");
          }

          if (!isActive) {
            return;
          }

          setCustomerProfile(profile);
          setOrganizerProfile(null);
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
          return;
        }

        if (userRole === "ORGANIZER") {
          const response = await getOrganizerProfile();
          const profile = response.data;

          if (!profile) {
            throw new Error("Organization profile not available.");
          }

          if (!isActive) {
            return;
          }

          setOrganizerProfile(profile);
          setCustomerProfile(null);
          setOrganizerForm({
            name: profile.name,
            description: profile.description ?? "",
            logoUrl: profile.logoUrl ?? "",
            coverUrl: profile.coverUrl ?? "",
          });

          if (profile.logoUrl) {
            updateUser({ avatarUrl: profile.logoUrl });
          }
        }
      } catch (error) {
        if (!isActive) {
          return;
        }

        if (
          userRole === "ORGANIZER" &&
          axios.isAxiosError(error) &&
          error.response?.status === 404
        ) {
          setIsMissingOrganization(true);
          setOrganizerProfile(null);
          return;
        }

        setErrorMessage(getApiErrorMessage(error));
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
  }, [userId, userRole, updateUser]);

  const canEdit =
    !isLoading &&
    (user?.role === "CUSTOMER"
      ? Boolean(customerProfile)
      : Boolean(organizerProfile) && !isMissingOrganization);

  const startEditing = () => {
    setIsEditing(true);
    setErrorMessage(null);
    setSuccessMessage(null);
  };

  const cancelEditing = () => {
    setIsEditing(false);
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsUploadingAvatar(false);

    if (user?.role === "CUSTOMER" && customerProfile) {
      setCustomerForm({
        fullName: customerProfile.fullName,
        phone: customerProfile.phone ?? "",
        address: customerProfile.address ?? "",
        avatarUrl: customerProfile.avatarUrl ?? "",
      });
    }

    if (user?.role === "ORGANIZER" && organizerProfile) {
      setOrganizerForm({
        name: organizerProfile.name,
        description: organizerProfile.description ?? "",
        logoUrl: organizerProfile.logoUrl ?? "",
        coverUrl: organizerProfile.coverUrl ?? "",
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
    setErrorMessage(null);
    setSuccessMessage(null);

    const input = event.target;

    try {
      const publicUrl = await uploadAvatarImage(file, user.id);
      setCustomerForm((current) => ({
        ...current,
        avatarUrl: publicUrl,
      }));
      setSuccessMessage("Avatar uploaded. Save changes to apply it.");
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error));
    } finally {
      setIsUploadingAvatar(false);
      input.value = "";
    }
  };

  const handleOrganizerChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setOrganizerForm((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const handleCustomerSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isUploadingAvatar) {
      setErrorMessage("Please wait for the avatar upload to finish.");
      return;
    }

    const fullName = customerForm.fullName.trim();
    if (!fullName) {
      setErrorMessage("Full name is required.");
      return;
    }

    setIsSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const response = await updateCustomerProfile({
        fullName,
        phone: toOptionalValue(customerForm.phone),
        address: toOptionalValue(customerForm.address),
        avatarUrl: toOptionalValue(customerForm.avatarUrl),
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
      updateUser({
        fullName: profile.fullName,
        avatarUrl: profile.avatarUrl ?? null,
      });
      setSuccessMessage(response.message || "Profile updated.");
      setIsEditing(false);
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error));
    } finally {
      setIsSaving(false);
    }
  };

  const handleOrganizerSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const name = organizerForm.name.trim();
    if (!name) {
      setErrorMessage("Organization name is required.");
      return;
    }

    setIsSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const response = await updateOrganizerProfile({
        name,
        description: toOptionalValue(organizerForm.description),
        logoUrl: toOptionalValue(organizerForm.logoUrl),
        coverUrl: toOptionalValue(organizerForm.coverUrl),
      });
      const profile = response.data;

      if (!profile) {
        throw new Error("Organization update failed.");
      }

      setOrganizerProfile(profile);
      setOrganizerForm({
        name: profile.name,
        description: profile.description ?? "",
        logoUrl: profile.logoUrl ?? "",
        coverUrl: profile.coverUrl ?? "",
      });
      if (profile.logoUrl) {
        updateUser({ avatarUrl: profile.logoUrl });
      }
      setSuccessMessage(response.message || "Organization updated.");
      setIsEditing(false);
    } catch (error) {
      if (
        axios.isAxiosError(error) &&
        error.response?.status === 404 &&
        user?.role === "ORGANIZER"
      ) {
        setIsMissingOrganization(true);
        setOrganizerProfile(null);
        setIsEditing(false);
      } else {
        setErrorMessage(getApiErrorMessage(error));
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleLogout = () => {
    clearSession();
    navigate("/");
  };

  if (!user) {
    return <Navigate replace state={{ from: returnTo }} to="/auth" />;
  }

  return (
    <main className="relative min-h-screen bg-canvas pb-16 pt-32">
      <AmbientBackgroundGradients />

      <div className="relative z-10 mx-auto flex w-full max-w-5xl flex-col gap-8 px-6">
        <header className="flex flex-wrap items-center gap-6 rounded-panel border border-white/60 bg-white/70 px-6 py-5 shadow-[0_20px_60px_rgba(16,24,40,0.12)] backdrop-blur">
          <div className="flex size-16 items-center justify-center overflow-hidden rounded-full bg-white shadow-[0_12px_30px_rgba(82,128,145,0.2)]">
            {user.avatarUrl ? (
              <img
                alt={user.fullName}
                className="size-full object-cover"
                src={user.avatarUrl}
              />
            ) : (
              <span className="font-fredoka text-lg font-bold text-black-blue">
                {avatarLabel}
              </span>
            )}
          </div>
          <div>
            <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
              Profile
            </p>
            <h1 className="mt-2 font-fredoka text-3xl font-bold text-black-blue">
              {user.fullName}
            </h1>
            <p className="mt-1 font-sans text-sm text-gray">
              {user.role === "CUSTOMER"
                ? "Customer account"
                : "Organizer account"}
            </p>
          </div>
          <Button
            className="ml-auto"
            disabled={isSaving}
            type="button"
            variant="outline"
            onClick={handleLogout}
          >
            Log out
          </Button>
        </header>

        {canEdit ? (
          <div className="flex flex-wrap items-center justify-between gap-4 rounded-panel border border-white/60 bg-white/70 px-6 py-5 shadow-[0_16px_40px_rgba(82,128,145,0.16)] backdrop-blur">
            <div>
              <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
                Profile details
              </p>
              <h2 className="mt-2 font-fredoka text-2xl font-bold text-black-blue">
                {user.role === "CUSTOMER"
                  ? "Customer information"
                  : "Organization information"}
              </h2>
              <p className="mt-1 font-sans text-sm text-gray">
                Keep your profile details up to date for a smoother experience.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              {isEditing ? (
                <>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={cancelEditing}
                    disabled={isSaving || isUploadingAvatar}
                  >
                    Cancel
                  </Button>
                  <Button
                    form={
                      user.role === "CUSTOMER"
                        ? "customer-profile-form"
                        : "organizer-profile-form"
                    }
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
          </div>
        ) : null}

        {isLoading ? (
          <div className="rounded-panel border border-white/60 bg-white/60 p-6 text-center font-sans text-sm text-gray shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            Loading your profile...
          </div>
        ) : null}

        {!isLoading && errorMessage ? (
          <div className="rounded-panel border border-peach bg-peach/20 p-6 font-sans text-sm text-black-blue">
            {errorMessage}
          </div>
        ) : null}

        {!isLoading && successMessage ? (
          <div className="rounded-panel border border-aqua bg-aqua/20 p-6 font-sans text-sm text-black-blue">
            {successMessage}
          </div>
        ) : null}

        {!isLoading && isMissingOrganization ? (
          <div className="rounded-panel border border-aqua bg-white/70 p-6 font-sans text-sm text-black-blue shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            No organization profile yet. Create one from the organizer dashboard
            when it is available.
          </div>
        ) : null}

        {!isLoading && user.role === "CUSTOMER" && customerProfile ? (
          isEditing ? (
            <form
              className="grid gap-4 md:grid-cols-2"
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
                  <div className="flex size-16 items-center justify-center overflow-hidden rounded-full bg-white shadow-[0_12px_30px_rgba(82,128,145,0.2)]">
                    {customerForm.avatarUrl ? (
                      <img
                        alt={customerForm.fullName}
                        className="size-full object-cover"
                        src={customerForm.avatarUrl}
                      />
                    ) : (
                      <span className="font-fredoka text-lg font-bold text-black-blue">
                        {avatarLabel}
                      </span>
                    )}
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
          ) : (
            <section className="grid gap-4 md:grid-cols-2">
              <InfoRow label="Email" value={customerProfile.email} />
              <InfoRow label="Phone" value={customerProfile.phone ?? ""} />
              <InfoRow label="Address" value={customerProfile.address ?? ""} />
              <InfoRow
                label="Member since"
                value={formatDate(customerProfile.createdAt)}
              />
            </section>
          )
        ) : null}

        {!isLoading && user.role === "ORGANIZER" && organizerProfile ? (
          isEditing ? (
            <form
              className="grid gap-4"
              id="organizer-profile-form"
              onSubmit={handleOrganizerSave}
            >
              <div className="grid gap-4 md:grid-cols-2">
                <Input
                  label="Organization name"
                  name="name"
                  onChange={handleOrganizerChange}
                  placeholder="Organization name"
                  required
                  value={organizerForm.name}
                />
                <Input
                  label="Status"
                  name="status"
                  placeholder=""
                  value={organizerProfile.status}
                  disabled
                />
                <div className="md:col-span-2">
                  <Input
                    label="Description"
                    name="description"
                    onChange={handleOrganizerChange}
                    placeholder="Optional"
                    value={organizerForm.description}
                  />
                </div>
                <Input
                  label="Logo URL"
                  name="logoUrl"
                  onChange={handleOrganizerChange}
                  placeholder="https://"
                  value={organizerForm.logoUrl}
                />
                <Input
                  label="Cover URL"
                  name="coverUrl"
                  onChange={handleOrganizerChange}
                  placeholder="https://"
                  value={organizerForm.coverUrl}
                />
              </div>
            </form>
          ) : (
            <section className="grid gap-6">
              <div className="overflow-hidden rounded-panel border border-white/60 bg-white/70 shadow-[0_20px_60px_rgba(16,24,40,0.12)]">
                {organizerProfile.coverUrl ? (
                  <img
                    alt={organizerProfile.name}
                    className="h-48 w-full object-cover"
                    src={organizerProfile.coverUrl}
                  />
                ) : (
                  <div className="h-48 w-full bg-brand-gradient" />
                )}
                <div className="grid gap-4 px-6 py-5 md:grid-cols-[auto_1fr]">
                  <div className="flex size-16 items-center justify-center overflow-hidden rounded-full bg-white shadow-[0_12px_30px_rgba(82,128,145,0.2)]">
                    {organizerProfile.logoUrl ? (
                      <img
                        alt=""
                        className="size-full object-cover"
                        src={organizerProfile.logoUrl}
                      />
                    ) : (
                      <span className="font-fredoka text-lg font-bold text-black-blue">
                        {organizerProfile.name.slice(0, 2).toUpperCase()}
                      </span>
                    )}
                  </div>
                  <div>
                    <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
                      Organization
                    </p>
                    <h2 className="mt-2 font-fredoka text-2xl font-bold text-black-blue">
                      {organizerProfile.name}
                    </h2>
                    <p className="mt-2 font-sans text-sm text-gray">
                      {organizerProfile.description ||
                        "No organization description yet."}
                    </p>
                  </div>
                </div>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <InfoRow label="Status" value={organizerProfile.status} />
                <InfoRow label="Owner ID" value={organizerProfile.ownerId} />
                <InfoRow
                  label="Created"
                  value={formatDate(organizerProfile.createdAt)}
                />
                <InfoRow
                  label="Updated"
                  value={formatDate(organizerProfile.updatedAt)}
                />
              </div>
            </section>
          )
        ) : null}
      </div>
    </main>
  );
}
