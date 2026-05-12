import { useState, type ChangeEvent, type FormEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { Button, Input } from "../components/ui";
import {
  getApiErrorMessage,
  login,
  registerCustomer,
  registerOrganizer,
  toAuthSession,
  verifyEmail,
} from "../api/auth";
import { useAuthStore } from "../stores/authStore";
import type { UserRole } from "../types/auth";

const accountTypes = [
  {
    label: "Customer",
    value: "CUSTOMER",
    description: "Browse, shop, and track orders.",
  },
  {
    label: "Organizer",
    value: "ORGANIZER",
    description: "Publish merch and manage events.",
  },
] as const;

type AuthMode = "signin" | "register" | "verify";
type AccountType = Extract<UserRole, "CUSTOMER" | "ORGANIZER">;

const initialFormState = {
  email: "",
  password: "",
  fullName: "",
  phone: "",
  address: "",
  otpCode: "",
};

export function AuthPage() {
  const [mode, setMode] = useState<AuthMode>("signin");
  const [accountType, setAccountType] = useState<AccountType>("CUSTOMER");
  const [formState, setFormState] = useState(initialFormState);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((state) => state.setSession);
  const returnTo = (location.state as { from?: string } | null)?.from ?? "/";
  const isVerifyMode = mode === "verify";
  const modeLabel =
    mode === "signin"
      ? "Sign in"
      : mode === "register"
        ? "Register"
        : "Verify email";
  const modeTitle =
    mode === "signin"
      ? "Access your account"
      : mode === "register"
        ? "Create an account"
        : "Confirm your email";
  const submitLabel =
    mode === "signin"
      ? "Sign in"
      : mode === "register"
        ? "Register"
        : "Verify email";

  const handleModeChange = (nextMode: AuthMode) => {
    setMode(nextMode);
    setErrorMessage(null);
    setSuccessMessage(null);
  };

  const handleAccountTypeChange = (nextType: AccountType) => {
    setAccountType(nextType);
    setErrorMessage(null);
    setSuccessMessage(null);
  };

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setFormState((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsSubmitting(true);

    try {
      if (mode === "verify") {
        const response = await verifyEmail({
          email: formState.email,
          otpCode: formState.otpCode,
        });

        setSuccessMessage(
          response.message || "Email verified. You can sign in now.",
        );
        setFormState((current) => ({
          ...current,
          password: "",
          otpCode: "",
        }));
        setMode("signin");
        return;
      }

      if (mode === "signin") {
        const response = await login({
          email: formState.email,
          password: formState.password,
        });

        if (!response.data) {
          throw new Error("Login failed. Please try again.");
        }

        const session = toAuthSession(response.data);
        setSession(session);

        if (session.user.role === "ORGANIZER") {
          navigate("/organizer");
          return;
        }

        navigate(returnTo);
        return;
      }

      const registerPayload = {
        email: formState.email,
        password: formState.password,
        fullName: formState.fullName,
        phone: formState.phone || undefined,
        address: formState.address || undefined,
      };

      const response =
        accountType === "ORGANIZER"
          ? await registerOrganizer(registerPayload)
          : await registerCustomer(registerPayload);

      setSuccessMessage(
        response.message ||
          "Registration successful. Check your email for the OTP.",
      );
      setFormState((current) => ({
        ...current,
        password: "",
        otpCode: "",
      }));
      setMode("verify");
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="relative min-h-screen bg-canvas pb-16 pt-32">
      <AmbientBackgroundGradients />

      <div className="relative z-10 mx-auto flex w-full max-w-5xl flex-col gap-8 px-6 lg:flex-row">
        <section className="flex flex-1 flex-col gap-6">
          <div className="rounded-panel border border-white/60 bg-white/50 p-6 shadow-[0_24px_80px_rgba(16,24,40,0.12)] backdrop-blur">
            <p className="font-google text-xs font-semibold uppercase tracking-[0.3em] text-slate/70">
              UITMerch Account
            </p>
            <h1 className="mt-4 font-brand text-3xl font-black text-black-blue sm:text-4xl">
              Welcome back to the merch studio.
            </h1>
            <p className="mt-4 max-w-lg font-google text-sm text-gray">
              Sign in to keep shopping or register as an organizer to prepare
              your next drop. Organizer dashboards will be available soon.
            </p>
            <div className="mt-6 grid gap-3">
              {accountTypes.map((type) => (
                <div
                  key={type.value}
                  className="rounded-2xl border border-white/70 bg-white/70 px-4 py-3 shadow-[0_12px_25px_rgba(82,128,145,0.12)]"
                >
                  <p className="font-brand text-lg font-black text-black-blue">
                    {type.label}
                  </p>
                  <p className="mt-1 font-google text-sm text-gray">
                    {type.description}
                  </p>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-panel border border-white/60 bg-white/40 p-5 text-sm text-gray shadow-[0_16px_40px_rgba(82,128,145,0.16)] backdrop-blur">
            <p className="font-google">
              After registration, check your email for the OTP to verify your
              account.
            </p>
          </div>
        </section>

        <section className="flex flex-1 flex-col">
          <div className="rounded-panel border border-white/70 bg-white/80 p-6 shadow-[0_20px_60px_rgba(16,24,40,0.16)] backdrop-blur">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="font-google text-xs uppercase tracking-[0.3em] text-slate/70">
                  {modeLabel}
                </p>
                <h2 className="mt-2 font-brand text-2xl font-black text-black-blue">
                  {modeTitle}
                </h2>
              </div>
              <div className="inline-flex rounded-full border border-white/70 bg-white/80 p-1">
                <button
                  className={[
                    "rounded-full px-4 py-2 text-sm font-google font-semibold transition",
                    mode === "signin"
                      ? "bg-brand-gradient text-black-blue shadow-[0_8px_20px_rgba(82,128,145,0.2)]"
                      : "text-gray",
                  ].join(" ")}
                  onClick={() => handleModeChange("signin")}
                  type="button"
                >
                  Sign in
                </button>
                <button
                  className={[
                    "rounded-full px-4 py-2 text-sm font-google font-semibold transition",
                    mode === "register"
                      ? "bg-brand-gradient text-black-blue shadow-[0_8px_20px_rgba(82,128,145,0.2)]"
                      : "text-gray",
                  ].join(" ")}
                  onClick={() => handleModeChange("register")}
                  type="button"
                >
                  Register
                </button>
                <button
                  className={[
                    "rounded-full px-4 py-2 text-sm font-google font-semibold transition",
                    mode === "verify"
                      ? "bg-brand-gradient text-black-blue shadow-[0_8px_20px_rgba(82,128,145,0.2)]"
                      : "text-gray",
                  ].join(" ")}
                  onClick={() => handleModeChange("verify")}
                  type="button"
                >
                  Verify
                </button>
              </div>
            </div>

            {!isVerifyMode ? (
              <div className="mt-6">
                <p className="font-google text-xs uppercase tracking-[0.3em] text-slate/70">
                  Account type
                </p>
                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  {accountTypes.map((type) => {
                    const isActive = accountType === type.value;

                    return (
                      <button
                        key={type.value}
                        className={[
                          "rounded-2xl border px-4 py-3 text-left transition",
                          isActive
                            ? "border-aqua bg-white shadow-[0_16px_40px_rgba(82,128,145,0.18)]"
                            : "border-white/60 bg-white/50 text-gray",
                        ].join(" ")}
                        onClick={() => handleAccountTypeChange(type.value)}
                        type="button"
                      >
                        <p className="font-brand text-lg font-black text-black-blue">
                          {type.label}
                        </p>
                        <p className="mt-1 font-google text-xs text-gray">
                          {type.description}
                        </p>
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : null}

            {errorMessage ? (
              <div className="mt-6 rounded-2xl border border-peach bg-peach/20 px-4 py-3 text-sm text-black-blue">
                {errorMessage}
              </div>
            ) : null}

            {successMessage ? (
              <div className="mt-6 rounded-2xl border border-aqua bg-aqua/20 px-4 py-3 text-sm text-black-blue">
                {successMessage}
              </div>
            ) : null}

            <form className="mt-6 grid gap-4" onSubmit={handleSubmit}>
              {mode === "register" ? (
                <Input
                  label="Full name"
                  name="fullName"
                  onChange={handleInputChange}
                  placeholder="Your full name"
                  required
                  value={formState.fullName}
                />
              ) : null}

              <Input
                label="Email"
                name="email"
                onChange={handleInputChange}
                placeholder="you@uit.edu.vn"
                required
                type="email"
                value={formState.email}
              />

              {!isVerifyMode ? (
                <Input
                  label="Password"
                  name="password"
                  onChange={handleInputChange}
                  placeholder="Password"
                  required
                  type="password"
                  value={formState.password}
                />
              ) : null}

              {isVerifyMode ? (
                <Input
                  label="OTP code"
                  name="otpCode"
                  onChange={handleInputChange}
                  placeholder="6-digit code"
                  required
                  value={formState.otpCode}
                />
              ) : null}

              {mode === "register" ? (
                <>
                  <Input
                    label="Phone"
                    name="phone"
                    onChange={handleInputChange}
                    placeholder="Optional"
                    value={formState.phone}
                  />
                  <Input
                    label="Address"
                    name="address"
                    onChange={handleInputChange}
                    placeholder="Optional"
                    value={formState.address}
                  />
                </>
              ) : null}

              <Button
                className="mt-2 w-full"
                loading={isSubmitting}
                type="submit"
              >
                {submitLabel}
              </Button>
            </form>
          </div>
        </section>
      </div>
    </main>
  );
}
