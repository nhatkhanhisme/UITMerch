import { useEffect, useMemo, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import axios from "axios";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { getApiErrorMessage } from "../api/auth";
import { getCustomerProfile, getOrganizerProfile } from "../api/profile";
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

type InfoRowProps = {
  label: string;
  value?: string | null;
};

function InfoRow({ label, value }: InfoRowProps) {
  return (
    <div className="rounded-2xl border border-white/70 bg-white/70 px-4 py-3 shadow-[0_10px_24px_rgba(82,128,145,0.12)]">
      <p className="font-google text-xs uppercase tracking-[0.3em] text-slate/60">
        {label}
      </p>
      <p className="mt-2 font-brand text-base font-black text-black-blue">
        {value && value.trim().length > 0 ? value : "N/A"}
      </p>
    </div>
  );
}

export function ProfilePage() {
  const user = useAuthStore((state) => state.user);
  const updateUser = useAuthStore((state) => state.updateUser);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isMissingOrganization, setIsMissingOrganization] = useState(false);
  const [customerProfile, setCustomerProfile] =
    useState<CustomerProfile | null>(null);
  const [organizerProfile, setOrganizerProfile] =
    useState<OrganizerProfile | null>(null);
  const location = useLocation();
  const returnTo = `${location.pathname}${location.search}${location.hash}`;

  const avatarLabel = useMemo(() => {
    if (!user?.fullName) {
      return "U";
    }

    return getInitials(user.fullName);
  }, [user?.fullName]);

  useEffect(() => {
    if (!user) {
      return;
    }

    let isActive = true;

    const loadProfile = async () => {
      setIsLoading(true);
      setErrorMessage(null);
      setIsMissingOrganization(false);

      try {
        if (user.role === "CUSTOMER") {
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
          updateUser({
            fullName: profile.fullName,
            avatarUrl: profile.avatarUrl ?? null,
          });
          return;
        }

        if (user.role === "ORGANIZER") {
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

          if (profile.logoUrl) {
            updateUser({ avatarUrl: profile.logoUrl });
          }
        }
      } catch (error) {
        if (!isActive) {
          return;
        }

        if (
          user.role === "ORGANIZER" &&
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
  }, [user, updateUser]);

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
              <span className="font-brand text-lg font-black text-black-blue">
                {avatarLabel}
              </span>
            )}
          </div>
          <div>
            <p className="font-google text-xs uppercase tracking-[0.3em] text-slate/70">
              Profile
            </p>
            <h1 className="mt-2 font-brand text-3xl font-black text-black-blue">
              {user.fullName}
            </h1>
            <p className="mt-1 font-google text-sm text-gray">
              {user.role === "CUSTOMER"
                ? "Customer account"
                : "Organizer account"}
            </p>
          </div>
        </header>

        {isLoading ? (
          <div className="rounded-panel border border-white/60 bg-white/60 p-6 text-center font-google text-sm text-gray shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            Loading your profile...
          </div>
        ) : null}

        {!isLoading && errorMessage ? (
          <div className="rounded-panel border border-peach bg-peach/20 p-6 font-google text-sm text-black-blue">
            {errorMessage}
          </div>
        ) : null}

        {!isLoading && isMissingOrganization ? (
          <div className="rounded-panel border border-aqua bg-white/70 p-6 font-google text-sm text-black-blue shadow-[0_16px_40px_rgba(82,128,145,0.16)]">
            No organization profile yet. Create one from the organizer dashboard
            when it is available.
          </div>
        ) : null}

        {!isLoading && user.role === "CUSTOMER" && customerProfile ? (
          <section className="grid gap-4 md:grid-cols-2">
            <InfoRow label="Email" value={customerProfile.email} />
            <InfoRow label="Phone" value={customerProfile.phone ?? ""} />
            <InfoRow label="Address" value={customerProfile.address ?? ""} />
            <InfoRow
              label="Member since"
              value={formatDate(customerProfile.createdAt)}
            />
          </section>
        ) : null}

        {!isLoading && user.role === "ORGANIZER" && organizerProfile ? (
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
                    <span className="font-brand text-lg font-black text-black-blue">
                      {organizerProfile.name.slice(0, 2).toUpperCase()}
                    </span>
                  )}
                </div>
                <div>
                  <p className="font-google text-xs uppercase tracking-[0.3em] text-slate/70">
                    Organization
                  </p>
                  <h2 className="mt-2 font-brand text-2xl font-black text-black-blue">
                    {organizerProfile.name}
                  </h2>
                  <p className="mt-2 font-google text-sm text-gray">
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
        ) : null}
      </div>
    </main>
  );
}
