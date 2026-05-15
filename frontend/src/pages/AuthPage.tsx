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
    label: "Khách hàng",
    value: "CUSTOMER",
    description: "Khám phá, mua sắm và theo dõi đơn hàng.",
  },
  {
    label: "Tổ chức",
    value: "ORGANIZER",
    description: "Đăng bán vật phẩm và quản lý sự kiện.",
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
      ? "Đăng nhập"
      : mode === "register"
        ? "Đăng ký"
        : "Xác thực email";
  const modeTitle =
    mode === "signin"
      ? "Truy cập tài khoản của bạn"
      : mode === "register"
        ? "Tạo tài khoản mới"
        : "Xác nhận email của bạn";
  const submitLabel =
    mode === "signin"
      ? "Đăng nhập"
      : mode === "register"
        ? "Đăng ký"
        : "Xác thực email";

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
          response.message || "Email đã xác thực. Bạn có thể đăng nhập ngay.",
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
          throw new Error("Đăng nhập thất bại. Vui lòng thử lại.");
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
          "Đăng ký thành công. Vui lòng kiểm tra email để lấy mã OTP.",
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
            <p className="font-sans text-xs font-semibold uppercase tracking-[0.3em] text-slate/70">
              UITMerch Account
            </p>
            <h1 className="mt-4 font-fredoka text-3xl font-bold text-black-blue sm:text-4xl">
              Chào mừng trở lại UITMerch.
            </h1>
            <p className="mt-4 max-w-lg font-sans text-sm text-gray">
              Đăng nhập để tiếp tục mua sắm hoặc đăng ký với tư cách ban tổ chức
              để chuẩn bị vật phẩm cho sự kiện tiếp theo.
            </p>
            <div className="mt-6">
              <p className="font-sans text-sm text-gray">
                Tạo tài khoản để bắt đầu mua sắm. Chọn vai trò trong quá trình
                đăng ký — khách hàng có thể xem và mua, ban tổ chức có thể đăng bán vật phẩm.
              </p>
            </div>
          </div>

          <div className="rounded-panel border border-white/60 bg-white/40 p-5 text-sm text-gray shadow-[0_16px_40px_rgba(82,128,145,0.16)] backdrop-blur">
            <p className="font-sans">
              Sau khi đăng ký, kiểm tra email để lấy mã OTP xác thực tài khoản.
            </p>
          </div>
        </section>

        <section className="flex flex-1 flex-col">
          <div className="rounded-panel border border-white/70 bg-white/80 p-6 shadow-[0_20px_60px_rgba(16,24,40,0.16)] backdrop-blur">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
                  {modeLabel}
                </p>
                <h2 className="mt-2 font-fredoka text-2xl font-bold text-black-blue">
                  {modeTitle}
                </h2>
              </div>
              <div className="inline-flex rounded-full border border-white/70 bg-white/80 p-1">
                <button
                  className={[
                    "rounded-full px-4 py-2 text-sm font-sans font-semibold transition",
                    mode === "signin"
                      ? "bg-brand-gradient text-black-blue shadow-[0_8px_20px_rgba(82,128,145,0.2)]"
                      : "text-gray",
                  ].join(" ")}
                  onClick={() => handleModeChange("signin")}
                  type="button"
                >
                  Đăng nhập
                </button>
                <button
                  className={[
                    "rounded-full px-4 py-2 text-sm font-sans font-semibold transition",
                    mode === "register"
                      ? "bg-brand-gradient text-black-blue shadow-[0_8px_20px_rgba(82,128,145,0.2)]"
                      : "text-gray",
                  ].join(" ")}
                  onClick={() => handleModeChange("register")}
                  type="button"
                >
                  Đăng ký
                </button>
                <button
                  className={[
                    "rounded-full px-4 py-2 text-sm font-sans font-semibold transition",
                    mode === "verify"
                      ? "bg-brand-gradient text-black-blue shadow-[0_8px_20px_rgba(82,128,145,0.2)]"
                      : "text-gray",
                  ].join(" ")}
                  onClick={() => handleModeChange("verify")}
                  type="button"
                >
                  Xác thực
                </button>
              </div>
            </div>

            {mode === "register" ? (
              <div className="mt-6">
                <p className="font-sans text-xs uppercase tracking-[0.3em] text-slate/70">
                  Loại tài khoản
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
                        <p className="font-fredoka text-lg font-bold text-black-blue">
                          {type.label}
                        </p>
                        <p className="mt-1 font-sans text-xs text-gray">
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
                  label="Họ và tên"
                  name="fullName"
                  onChange={handleInputChange}
                  placeholder="Nhập họ và tên của bạn"
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
                  label="Mật khẩu"
                  name="password"
                  onChange={handleInputChange}
                  placeholder="Mật khẩu"
                  required
                  type="password"
                  value={formState.password}
                />
              ) : null}

              {isVerifyMode ? (
                <Input
                  label="Mã OTP"
                  name="otpCode"
                  onChange={handleInputChange}
                  placeholder="Mã 6 chữ số"
                  required
                  value={formState.otpCode}
                />
              ) : null}

              {mode === "register" ? (
                <>
                  {/* Keep registration minimal: only full name and email/password by default */}
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
