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
import { uploadOrganizerImage } from "../api/storage";
import { getOrganizerProfile, updateOrganizerProfile } from "../api/profile";
import { useAuthStore } from "../stores/authStore";
import { toast } from "../stores/toastStore";
import type { OrganizerProfile } from "../types/profile";
import {
  formatDate,
  getInitials,
  ProfileInfoRow,
  toOptionalValue,
} from "./profileUtils";

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
        const response = await getOrganizerProfile();
        const profile = response.data;

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
        if (!isActive) {
          return;
        }

        toast.error(getApiErrorMessage(error));
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

  const handleOrganizerChange = (event: ChangeEvent<HTMLInputElement>) => {
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
      toast.success("Logo uploaded. Save changes to apply it.");
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
    toast.info("Logo removed. Save changes to apply it.");
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
      toast.success("Cover uploaded. Save changes to apply it.");
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
    toast.info("Cover removed. Save changes to apply it.");
  };

  const handleOrganizerSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isUploadingMedia) {
      toast.error("Please wait for the image upload to finish.");
      return;
    }

    const name = organizerForm.name.trim();
    if (!name) {
      toast.error("Organization name is required.");
      return;
    }

    setIsSaving(true);

    try {
      const response = await updateOrganizerProfile({
        name,
        description: toOptionalValue(organizerForm.description),
        logoUrl: isLogoRemoved ? "" : toOptionalValue(organizerForm.logoUrl),
        coverUrl: isCoverRemoved ? "" : toOptionalValue(organizerForm.coverUrl),
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
      setIsLogoRemoved(false);
      setIsCoverRemoved(false);
      updateUser({ avatarUrl: profile.logoUrl ?? null });
      toast.success(response.message || "Organization updated.");
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

  return (
    <main className="relative min-h-screen bg-canvas pb-16 pt-28 sm:pt-32">
      <AmbientBackgroundGradients />

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-6 px-6">
        <section className="overflow-hidden rounded-[36px] border border-white/60 bg-white/75 shadow-[0_24px_80px_rgba(16,24,40,0.12)] backdrop-blur">
          <div className="grid gap-0 lg:grid-cols-[0.95fr_1.05fr]">
            <div className="relative min-h-[280px] bg-gradient-to-br from-black-blue via-slate to-aqua/70 p-6 text-white sm:p-8 lg:p-9">
              {organizerProfile?.coverUrl ? (
                <img
                  alt={organizerProfile.name}
                  className="absolute inset-0 h-full w-full object-cover opacity-35"
                  src={organizerProfile.coverUrl}
                />
              ) : null}
              <div className="relative z-10 flex h-full flex-col justify-between gap-8">
                <div className="flex items-center justify-between gap-4">
                  <div className="inline-flex items-center rounded-full border border-white/20 bg-white/10 px-3 py-1 font-sans text-[11px] font-semibold uppercase tracking-[0.28em] text-white/80 backdrop-blur-sm">
                    Organizer profile
                  </div>
                  <Button
                    className="shrink-0 border-white/30 bg-white/10 text-white hover:bg-white/20"
                    disabled={isSaving}
                    type="button"
                    variant="outline"
                    onClick={handleLogout}
                  >
                    Log out
                  </Button>
                </div>

                <div className="flex flex-wrap items-end gap-4">
                  <div className="flex size-20 items-center justify-center overflow-hidden rounded-full border border-white/30 bg-white/15 shadow-[0_12px_30px_rgba(0,0,0,0.16)] backdrop-blur-sm">
                    {organizerProfile?.logoUrl || user.avatarUrl ? (
                      <img
                        alt={organizerProfile?.name ?? user.fullName}
                        className="size-full object-cover"
                        src={organizerProfile?.logoUrl ?? user.avatarUrl ?? ""}
                      />
                    ) : (
                      <span className="font-brand text-2xl font-black text-white">
                        {avatarLabel}
                      </span>
                    )}
                  </div>
                  <div className="min-w-0">
                    <p className="font-sans text-xs uppercase tracking-[0.3em] text-white/70">
                      Organization workspace
                    </p>
                    <h1 className="mt-3 font-brand text-3xl font-black text-white sm:text-4xl">
                      {organizerProfile?.name ?? user.fullName}
                    </h1>
                    <p className="mt-2 max-w-2xl font-google text-sm leading-6 text-white/80">
                      Manage your merch identity with a cleaner overview for
                      logo, cover art, and organization details.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="p-6 sm:p-8 lg:p-9">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
                    Organization details
                  </p>
                  <h2 className="mt-2 font-brand text-2xl font-black text-black-blue">
                    {isEditing ? "Edit mode" : "Overview"}
                  </h2>
                </div>
                {canEdit ? (
                  isEditing ? (
                    <div className="flex flex-wrap items-center gap-3">
                      {hasUnsavedMediaChange ? (
                        <div className="inline-flex items-center rounded-full border border-gold/60 bg-gold/20 px-3 py-1.5 font-sans text-xs font-semibold text-black-blue shadow-glass">
                          ● Unsaved image change
                        </div>
                      ) : null}
                      <Button
                        type="button"
                        variant="outline"
                        onClick={cancelEditing}
                        disabled={isSaving || isUploadingMedia}
                      >
                        Cancel
                      </Button>
                      <Button
                        form="organizer-profile-form"
                        loading={isSaving}
                        type="submit"
                        disabled={isSaving || isUploadingMedia}
                      >
                        Save changes
                      </Button>
                    </div>
                  ) : (
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={startEditing}
                    >
                      Edit profile
                    </Button>
                  )
                ) : null}
              </div>

              <div className="mt-6 grid gap-3">
                <ProfileInfoRow
                  label="Status"
                  value={organizerProfile?.status ?? "Pending"}
                  description="Current organization state."
                  leading="S"
                />
                <ProfileInfoRow
                  label="Owner ID"
                  value={organizerProfile?.ownerId ?? "N/A"}
                  description="Linked account owner."
                  leading="#"
                />
                <ProfileInfoRow
                  label="Updated"
                  value={formatDate(organizerProfile?.updatedAt)}
                  description="Last profile change."
                  leading="U"
                />
              </div>
            </div>
          </div>
        </section>

        {isLoading ? (
          <div className="rounded-panel border border-white/60 bg-white/60 p-6 text-center font-sans text-sm text-gray shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            Loading your organization profile...
          </div>
        ) : null}



        {!isLoading && isMissingOrganization ? (
          <div className="rounded-panel border border-aqua bg-white/70 p-6 font-sans text-sm text-black-blue shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            No organization profile yet. Create one from the organizer dashboard
            when it is available.
          </div>
        ) : null}

        {!isLoading && organizerProfile && isEditing ? (
          <form
            className="grid gap-4 rounded-[32px] border border-white/60 bg-white/75 p-6 shadow-[0_20px_60px_rgba(16,24,40,0.12)] backdrop-blur-sm"
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
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label
                  className="font-sans text-sm text-gray"
                  htmlFor="org-logo-upload"
                >
                  Logo image
                </label>
                <div className="mt-2 flex flex-wrap items-center gap-4">
                  <div className="relative flex size-16 items-center justify-center overflow-hidden rounded-full bg-white shadow-[0_12px_30px_rgba(82,128,145,0.2)]">
                    {organizerForm.logoUrl ? (
                      <img
                        alt="Organization logo"
                        className="size-full object-cover"
                        src={organizerForm.logoUrl}
                      />
                    ) : (
                      <span className="font-brand text-lg font-black text-black-blue">
                        {organizerForm.name
                          ? organizerForm.name.slice(0, 2).toUpperCase()
                          : "OR"}
                      </span>
                    )}
                    {organizerForm.logoUrl ? (
                      <button
                        aria-label="Remove logo"
                        className="absolute right-0 top-0 flex size-6 -translate-y-1/3 translate-x-1/3 items-center justify-center rounded-full border border-white/70 bg-peach text-xs font-bold text-black-blue shadow-glass"
                        disabled={isSaving || isUploadingMedia}
                        onClick={handleOrganizerLogoRemove}
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
                      disabled={isSaving || isUploadingMedia}
                      id="org-logo-upload"
                      onChange={handleOrganizerLogoUpload}
                      type="file"
                    />
                    <p className="mt-2 font-sans text-xs text-gray">
                      {isUploadingLogo
                        ? "Uploading logo..."
                        : "PNG, JPG, GIF, SVG, or WEBP up to 10MB."}
                    </p>
                  </div>
                </div>
              </div>

              <div>
                <label
                  className="font-sans text-sm text-gray"
                  htmlFor="org-cover-upload"
                >
                  Cover image
                </label>
                <div className="mt-2 grid gap-3">
                  <div className="relative h-40 w-full overflow-hidden rounded-panel border border-white/60 bg-white/70 shadow-[0_12px_30px_rgba(82,128,145,0.2)]">
                    {organizerForm.coverUrl ? (
                      <img
                        alt="Organization cover"
                        className="h-full w-full object-cover"
                        src={organizerForm.coverUrl}
                      />
                    ) : (
                      <div className="h-full w-full bg-brand-gradient" />
                    )}
                    {organizerForm.coverUrl ? (
                      <button
                        aria-label="Remove cover"
                        className="absolute right-3 top-3 flex size-7 items-center justify-center rounded-full border border-white/70 bg-peach text-xs font-bold text-black-blue shadow-glass"
                        disabled={isSaving || isUploadingMedia}
                        onClick={handleOrganizerCoverRemove}
                        type="button"
                      >
                        x
                      </button>
                    ) : null}
                  </div>
                  <input
                    accept="image/*"
                    className="h-12 w-full rounded-glass border border-white/40 bg-white/20 px-4 font-sans text-sm text-ink shadow-glass transition duration-200 file:mr-4 file:rounded-glass file:border-0 file:bg-aqua file:px-4 file:py-2 file:font-sans file:text-sm file:font-semibold file:text-black-blue hover:file:bg-gold"
                    disabled={isSaving || isUploadingMedia}
                    id="org-cover-upload"
                    onChange={handleOrganizerCoverUpload}
                    type="file"
                  />
                  <p className="font-sans text-xs text-gray">
                    {isUploadingCover
                      ? "Uploading cover..."
                      : "PNG, JPG, GIF, SVG, or WEBP up to 10MB."}
                  </p>
                </div>
              </div>
            </div>
          </form>
        ) : null}

        {!isLoading && organizerProfile && !isEditing ? (
          <section className="overflow-hidden rounded-[32px] border border-white/60 bg-white/75 shadow-[0_20px_60px_rgba(16,24,40,0.12)] backdrop-blur-sm">
            <div className="grid gap-0 lg:grid-cols-[0.95fr_1.05fr]">
              <div className="p-6 sm:p-8">
                <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
                  Organization overview
                </p>
                <h3 className="mt-3 font-brand text-2xl font-black text-black-blue">
                  {organizerProfile.name}
                </h3>
                <p className="mt-3 font-google text-sm leading-6 text-gray">
                  {organizerProfile.description ||
                    "No organization description yet."}
                </p>
              </div>
              <div className="border-t border-white/60 bg-gradient-to-br from-aqua/10 to-white p-6 sm:p-8 lg:border-l lg:border-t-0">
                <div className="grid gap-3 sm:grid-cols-2">
                  <ProfileInfoRow
                    label="Created"
                    value={formatDate(organizerProfile.createdAt)}
                    description="First profile creation."
                    leading="C"
                  />
                  <ProfileInfoRow
                    label="Logo"
                    value={organizerProfile.logoUrl ? "Uploaded" : "Missing"}
                    description="Brand identity image."
                    leading="L"
                  />
                  <ProfileInfoRow
                    label="Cover"
                    value={organizerProfile.coverUrl ? "Uploaded" : "Missing"}
                    description="Main banner image."
                    leading="B"
                  />
                  <ProfileInfoRow
                    label="Role"
                    value="Organizer"
                    description="Manage merch and organization assets."
                    leading="O"
                  />
                </div>
              </div>
            </div>
          </section>
        ) : null}
      </div>
    </main>
  );
}
