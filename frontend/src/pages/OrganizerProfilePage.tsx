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
      if (!organizerProfile?.id) throw new Error("Organization ID not found.");
      const response = await updateOrganizerProfile(organizerProfile.id, {
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

  // Resolve which logo/cover to show in the left hero panel
  const displayedLogoUrl = isEditing
    ? organizerForm.logoUrl
    : (organizerProfile?.logoUrl ?? user.avatarUrl ?? "");
  const displayedCoverUrl = isEditing
    ? organizerForm.coverUrl
    : (organizerProfile?.coverUrl ?? "");

  return (
    <main className="relative min-h-screen bg-canvas pb-16 pt-28 sm:pt-32">
      <AmbientBackgroundGradients />

      <div className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-6 px-6">
        {/* ── Loading ── */}
        {isLoading ? (
          <div className="rounded-panel border border-white/60 bg-white/60 p-6 text-center font-sans text-sm text-gray shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            Loading your organization profile...
          </div>
        ) : null}

        {/* ── Missing org notice ── */}
        {!isLoading && isMissingOrganization ? (
          <div className="rounded-panel border border-aqua bg-white/70 p-6 font-sans text-sm text-black-blue shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            No organization profile yet. Create one from the organizer dashboard
            when it is available.
          </div>
        ) : null}

        {/* ── Main unified card ── */}
        {!isLoading && !isMissingOrganization ? (
          <form
            className="overflow-hidden rounded-[36px] border border-white/60 bg-white/75 shadow-[0_24px_80px_rgba(16,24,40,0.12)] backdrop-blur"
            id="organizer-profile-form"
            onSubmit={handleOrganizerSave}
          >
            <div className="grid gap-0 lg:grid-cols-2">
              {/* ── Left: Cover hero + Logo identity ── */}
              <div className="relative flex flex-col bg-gradient-to-br from-black-blue via-slate to-aqua/70 text-white min-w-0 min-h-[360px]">
                {/* Cover image */}
                <div className="relative h-48 w-full overflow-hidden sm:h-56">
                  {displayedCoverUrl ? (
                    <img
                      alt="Organization cover"
                      className="h-full w-full object-cover opacity-70"
                      src={displayedCoverUrl}
                    />
                  ) : (
                    <div className="h-full w-full bg-gradient-to-br from-black-blue via-slate to-aqua/70" />
                  )}
                  {/* Cover remove + choose file controls in edit mode */}
                  {isEditing ? (
                    <div className="absolute bottom-3 right-3 flex flex-col items-end gap-1">
                      {organizerForm.coverUrl ? (
                        <button
                          aria-label="Remove cover"
                          className="flex size-7 items-center justify-center rounded-full border border-white/70 bg-peach text-xs font-bold text-black-blue shadow-glass hover:scale-110 transition z-10"
                          disabled={isSaving || isUploadingMedia}
                          onClick={handleOrganizerCoverRemove}
                          type="button"
                        >
                          ✕
                        </button>
                      ) : null}
                      <label
                        className="inline-flex cursor-pointer items-center gap-1.5 rounded-full border border-white/30 bg-black-blue/60 px-3 py-1 font-sans text-xs font-semibold text-white shadow-sm transition hover:bg-black-blue/80 backdrop-blur-sm"
                        htmlFor="org-cover-upload"
                      >
                        <ImageIcon />
                        <span className="text-white">{isUploadingCover ? "Uploading..." : "Change cover"}</span>
                      </label>
                      <input
                        accept="image/*"
                        className="hidden"
                        disabled={isSaving || isUploadingMedia}
                        id="org-cover-upload"
                        onChange={handleOrganizerCoverUpload}
                        type="file"
                      />
                    </div>
                  ) : null}
                </div>

                {/* Logo + name area below cover */}
                <div className="relative flex flex-1 flex-col gap-4 p-6 sm:p-8">
                  <div className="flex flex-wrap items-end gap-4 -mt-10">
                    {/* Logo circle stacked with chooser */}
                    <div className="flex flex-col items-center gap-2">
                      <div className="relative inline-block">
                        <div className="relative flex size-20 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-white/30 bg-white/15 shadow-[0_12px_30px_rgba(0,0,0,0.24)] backdrop-blur-sm">
                          {displayedLogoUrl ? (
                            <img
                              alt={organizerForm.name || "Organization logo"}
                              className="size-full object-cover"
                              src={displayedLogoUrl}
                            />
                          ) : (
                            <span className="font-brand text-2xl font-black text-white">
                              {organizerForm.name
                                ? organizerForm.name.slice(0, 2).toUpperCase()
                                : avatarLabel}
                            </span>
                          )}
                        </div>
                        {isEditing && organizerForm.logoUrl ? (
                          <button
                            aria-label="Remove logo"
                            className="absolute right-0 top-0 flex size-6 -translate-y-1/3 translate-x-1/3 items-center justify-center rounded-full border border-white/70 bg-peach text-xs font-bold text-black-blue shadow-glass z-10 hover:scale-110 transition"
                            disabled={isSaving || isUploadingMedia}
                            onClick={handleOrganizerLogoRemove}
                            type="button"
                          >
                            ✕
                          </button>
                        ) : null}
                      </div>
                      {isEditing ? (
                        <div className="flex flex-col items-center gap-1">
                          <label
                            className="inline-flex cursor-pointer items-center gap-1.5 rounded-full border border-white/30 bg-white/15 px-3 py-1 font-sans text-xs font-semibold text-white shadow-sm transition hover:bg-white/25 backdrop-blur-sm"
                            htmlFor="org-logo-upload"
                          >
                            <CameraIcon />
                            <span className="text-white">{isUploadingLogo ? "Uploading..." : "Logo"}</span>
                          </label>
                          <input
                            accept="image/*"
                            className="hidden"
                            disabled={isSaving || isUploadingMedia}
                            id="org-logo-upload"
                            onChange={handleOrganizerLogoUpload}
                            type="file"
                          />
                        </div>
                      ) : null}
                    </div>

                    <div className="min-w-0 flex-1">
                      <div className="inline-flex items-center rounded-full border border-white/20 bg-white/10 px-3 py-1 font-sans text-[11px] font-semibold uppercase tracking-[0.28em] text-white/80 backdrop-blur-sm">
                        Organizer profile
                      </div>
                      {isEditing ? (
                        <div className="mt-3 max-w-sm">
                          <Input
                            label="Organization name"
                            name="name"
                            onChange={handleOrganizerChange}
                            placeholder="Organization name"
                            required
                            value={organizerForm.name}
                            disabled={isSaving || isUploadingMedia}
                          />
                        </div>
                      ) : (
                        <h1 className="mt-3 font-brand text-3xl font-black text-white sm:text-4xl break-words">
                          {organizerProfile?.name ?? user.fullName}
                        </h1>
                      )}

                      {/* Minimalist metadata */}
                      <div className="mt-3 flex flex-wrap items-center gap-4 text-sm text-white/70">
                        <div className="flex items-center gap-1.5">
                          <ShieldIcon />
                          <span className="text-white/80">{organizerProfile?.status ?? "Pending"}</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <CalendarIcon />
                          <span>Since {formatDate(organizerProfile?.createdAt)}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* ── Right: Actions + Inline Fields ── */}
              <div className="p-6 sm:p-8 lg:p-9 border-t border-white/60 lg:border-t-0 lg:border-l min-w-0">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
                      Organization details
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

                {/* Dynamic field presentation */}
                <div className="mt-6">
                  {isEditing ? (
                    <div className="grid gap-4">
                      <Input
                        label="Description"
                        name="description"
                        onChange={handleOrganizerChange}
                        placeholder="Tell people about your organization (optional)"
                        value={organizerForm.description}
                        disabled={isSaving || isUploadingMedia}
                      />
                    </div>
                  ) : (
                    <div className="grid gap-3">
                      <ProfileInfoRow
                        label="Description"
                        value={organizerProfile?.description ?? ""}
                        description="About your organization."
                        leading="D"
                      />
                      <ProfileInfoRow
                        label="Updated"
                        value={formatDate(organizerProfile?.updatedAt)}
                        description="Last profile change."
                        leading="U"
                      />
                    </div>
                  )}
                </div>
              </div>
            </div>
          </form>
        ) : null}
      </div>
    </main>
  );
}
