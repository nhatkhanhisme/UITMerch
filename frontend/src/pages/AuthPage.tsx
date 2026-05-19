import { useState, type ChangeEvent, type FormEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { Button, Input } from "../components/ui";
import {
  forgotPassword,
  getApiErrorMessage,
  login,
  registerCustomer,
  registerOrganizer,
  resendOtp,
  resetPassword,
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

type AuthMode = "signin" | "register";
type AccountType = Extract<UserRole, "CUSTOMER" | "ORGANIZER">;

const initialFormState = {
  email: "",
  password: "",
  confirmPassword: "",
  fullName: "",
  phone: "",
  otpCode: "",
};

export function AuthPage() {
  const [mode, setMode] = useState<AuthMode>("signin");
  const [accountType, setAccountType] = useState<AccountType>("CUSTOMER");
  const [formState, setFormState] = useState(initialFormState);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showOtpModal, setShowOtpModal] = useState(false);
  const [otpEmail, setOtpEmail] = useState("");
  const [otpCode, setOtpCode] = useState("");
  const [otpError, setOtpError] = useState<string | null>(null);
  const [otpSuccess, setOtpSuccess] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [showForgotModal, setShowForgotModal] = useState(false);
  const [forgotStep, setForgotStep] = useState<"email" | "reset">("email");
  const [forgotEmail, setForgotEmail] = useState("");
  const [resetOtpCode, setResetOtpCode] = useState("");
  const [resetNewPassword, setResetNewPassword] = useState("");
  const [resetConfirmPassword, setResetConfirmPassword] = useState("");
  const [isSendingForgot, setIsSendingForgot] = useState(false);
  const [isResettingPassword, setIsResettingPassword] = useState(false);
  const [forgotError, setForgotError] = useState<string | null>(null);
  const [forgotSuccess, setForgotSuccess] = useState<string | null>(null);
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((state) => state.setSession);
  const returnTo = (location.state as { from?: string } | null)?.from ?? "/";
  const modeLabel = mode === "signin" ? "Đăng nhập" : "Đăng ký";
  const modeTitle =
    mode === "signin"
      ? "Truy cập tài khoản của bạn"
      : "Tạo tài khoản mới";
  const submitLabel = mode === "signin" ? "Đăng nhập" : "Đăng ký";

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

      if (formState.password !== formState.confirmPassword) {
        setErrorMessage("Mật khẩu xác nhận không khớp.");
        setIsSubmitting(false);
        return;
      }

      const registerPayload = {
        email: formState.email,
        password: formState.password,
        fullName: formState.fullName,
        phone: formState.phone || undefined,
      };

      const response =
        accountType === "ORGANIZER"
          ? await registerOrganizer(registerPayload)
          : await registerCustomer(registerPayload);

      setOtpEmail(formState.email);
      setOtpCode("");
      setOtpError(null);
      setOtpSuccess(response.message || "Vui lòng kiểm tra email để lấy mã OTP.");
      setShowOtpModal(true);
      setFormState((current) => ({ ...current, password: "" }));
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

              <Input
                label="Mật khẩu"
                name="password"
                onChange={handleInputChange}
                placeholder="Mật khẩu"
                required
                type="password"
                value={formState.password}
              />

              {mode === "signin" ? (
                <div className="-mt-2 text-right">
                  <button
                    className="font-sans text-xs text-gray transition hover:text-black-blue"
                    onClick={() => {
                      setForgotEmail("");
                      setForgotStep("email");
                      setForgotError(null);
                      setForgotSuccess(null);
                      setShowForgotModal(true);
                    }}
                    type="button"
                  >
                    Quên mật khẩu?
                  </button>
                </div>
              ) : null}

              {mode === "register" ? (
                <Input
                  label="Xác nhận mật khẩu"
                  name="confirmPassword"
                  onChange={handleInputChange}
                  placeholder="Nhập lại mật khẩu"
                  required
                  type="password"
                  value={formState.confirmPassword}
                />
              ) : null}

              {mode === "register" && accountType === "CUSTOMER" ? (
                <Input
                  label="Số điện thoại"
                  name="phone"
                  onChange={handleInputChange}
                  placeholder="Nhập số điện thoại của bạn"
                  type="tel"
                  value={formState.phone}
                />
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

      {showForgotModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4 backdrop-blur-sm">
          <div className="w-full max-w-sm rounded-panel border border-white/70 bg-white/95 p-6 shadow-2xl backdrop-blur">
            <div className="mb-5 text-center">
              <div className="mx-auto mb-3 flex size-12 items-center justify-center rounded-full bg-aqua/20 text-2xl">
                🔑
              </div>
              <h2 className="font-fredoka text-2xl font-bold text-black-blue">Đặt lại mật khẩu</h2>
              <p className="mt-1 text-sm text-gray">
                {forgotStep === "email"
                  ? "Nhập email để nhận mã OTP khôi phục."
                  : <>Mã OTP đã được gửi đến <strong>{forgotEmail}</strong></>}
              </p>
            </div>

            {forgotError ? (
              <div className="mb-4 rounded-xl border border-peach bg-peach/20 px-3 py-2 text-sm text-black-blue">
                {forgotError}
              </div>
            ) : null}

            {forgotSuccess ? (
              <div className="mb-4 rounded-xl border border-aqua bg-aqua/20 px-3 py-2 text-sm text-black-blue">
                {forgotSuccess}
              </div>
            ) : null}

            {forgotStep === "email" ? (
              <div className="grid gap-3">
                <div>
                  <label className="mb-1 block font-sans text-xs font-semibold text-slate/70">
                    Email
                  </label>
                  <input
                    autoFocus
                    className="w-full rounded-2xl border border-white/70 bg-white/70 px-4 py-3 text-sm text-black-blue shadow-sm focus:outline-none focus:ring-2 focus:ring-aqua"
                    onChange={(e) => setForgotEmail(e.target.value)}
                    placeholder="you@uit.edu.vn"
                    type="email"
                    value={forgotEmail}
                  />
                </div>
                <Button
                  className="w-full"
                  loading={isSendingForgot}
                  onClick={async () => {
                    if (!forgotEmail.trim()) {
                      setForgotError("Vui lòng nhập email.");
                      return;
                    }
                    setForgotError(null);
                    setForgotSuccess(null);
                    setIsSendingForgot(true);
                    try {
                      await forgotPassword(forgotEmail.trim());
                      setResetOtpCode("");
                      setResetNewPassword("");
                      setResetConfirmPassword("");
                      setForgotSuccess("Mã OTP đã được gửi. Kiểm tra hộp thư của bạn.");
                      setForgotStep("reset");
                    } catch (err) {
                      setForgotError(getApiErrorMessage(err, "Không thể gửi mã OTP. Thử lại sau."));
                    } finally {
                      setIsSendingForgot(false);
                    }
                  }}
                  type="button"
                >
                  Gửi mã OTP
                </Button>
                <button
                  className="font-sans text-sm text-gray transition hover:text-black-blue"
                  onClick={() => setShowForgotModal(false)}
                  type="button"
                >
                  Hủy
                </button>
              </div>
            ) : (
              <div className="grid gap-3">
                <div>
                  <label className="mb-1 block font-sans text-xs font-semibold text-slate/70">
                    Mã OTP (6 chữ số)
                  </label>
                  <input
                    autoFocus
                    className="w-full rounded-2xl border border-white/70 bg-white/70 px-4 py-3 text-center text-2xl font-bold tracking-[0.5em] text-black-blue shadow-sm focus:outline-none focus:ring-2 focus:ring-aqua"
                    maxLength={6}
                    onChange={(e) => setResetOtpCode(e.target.value.replace(/\D/g, ""))}
                    placeholder="______"
                    type="text"
                    value={resetOtpCode}
                  />
                </div>
                <div>
                  <label className="mb-1 block font-sans text-xs font-semibold text-slate/70">
                    Mật khẩu mới
                  </label>
                  <input
                    className="w-full rounded-2xl border border-white/70 bg-white/70 px-4 py-3 text-sm text-black-blue shadow-sm focus:outline-none focus:ring-2 focus:ring-aqua"
                    onChange={(e) => setResetNewPassword(e.target.value)}
                    placeholder="Mật khẩu mới"
                    type="password"
                    value={resetNewPassword}
                  />
                </div>
                <div>
                  <label className="mb-1 block font-sans text-xs font-semibold text-slate/70">
                    Xác nhận mật khẩu mới
                  </label>
                  <input
                    className="w-full rounded-2xl border border-white/70 bg-white/70 px-4 py-3 text-sm text-black-blue shadow-sm focus:outline-none focus:ring-2 focus:ring-aqua"
                    onChange={(e) => setResetConfirmPassword(e.target.value)}
                    placeholder="Nhập lại mật khẩu mới"
                    type="password"
                    value={resetConfirmPassword}
                  />
                </div>
                <Button
                  className="w-full"
                  loading={isResettingPassword}
                  onClick={async () => {
                    if (resetOtpCode.length !== 6) {
                      setForgotError("Vui lòng nhập đủ 6 chữ số OTP.");
                      return;
                    }
                    if (!resetNewPassword || resetNewPassword.length < 6) {
                      setForgotError("Mật khẩu mới phải có ít nhất 6 ký tự.");
                      return;
                    }
                    if (resetNewPassword !== resetConfirmPassword) {
                      setForgotError("Mật khẩu xác nhận không khớp.");
                      return;
                    }
                    setForgotError(null);
                    setForgotSuccess(null);
                    setIsResettingPassword(true);
                    try {
                      await resetPassword({ email: forgotEmail, otpCode: resetOtpCode, newPassword: resetNewPassword });
                      setShowForgotModal(false);
                      setMode("signin");
                      setSuccessMessage("Mật khẩu đã được đặt lại. Bạn có thể đăng nhập ngay.");
                    } catch (err) {
                      setForgotError(getApiErrorMessage(err, "Không thể đặt lại mật khẩu. Kiểm tra mã OTP và thử lại."));
                    } finally {
                      setIsResettingPassword(false);
                    }
                  }}
                  type="button"
                >
                  Đổi mật khẩu
                </Button>
                <button
                  className="font-sans text-sm text-gray transition hover:text-black-blue"
                  onClick={() => {
                    setForgotStep("email");
                    setForgotError(null);
                    setForgotSuccess(null);
                  }}
                  type="button"
                >
                  ← Dùng email khác
                </button>
              </div>
            )}
          </div>
        </div>
      ) : null}

      {showOtpModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4 backdrop-blur-sm">
          <div className="w-full max-w-sm rounded-panel border border-white/70 bg-white/95 p-6 shadow-2xl backdrop-blur">
            <div className="text-center mb-5">
              <div className="mx-auto mb-3 flex size-12 items-center justify-center rounded-full bg-aqua/20 text-2xl">
                ✉
              </div>
              <h2 className="font-fredoka text-2xl font-bold text-black-blue">Xác thực email</h2>
              <p className="mt-1 text-sm text-gray">
                Mã OTP đã được gửi đến <strong>{otpEmail}</strong>
              </p>
            </div>

            {otpError ? (
              <div className="mb-4 rounded-xl border border-peach bg-peach/20 px-3 py-2 text-sm text-black-blue">
                {otpError}
              </div>
            ) : null}

            {otpSuccess ? (
              <div className="mb-4 rounded-xl border border-aqua bg-aqua/20 px-3 py-2 text-sm text-black-blue">
                {otpSuccess}
              </div>
            ) : null}

            <div className="grid gap-3">
              <div>
                <label className="mb-1 block font-sans text-xs font-semibold text-slate/70">
                  Mã OTP (6 chữ số)
                </label>
                <input
                  autoFocus
                  className="w-full rounded-2xl border border-white/70 bg-white/70 px-4 py-3 text-center text-2xl font-bold tracking-[0.5em] text-black-blue shadow-sm focus:outline-none focus:ring-2 focus:ring-aqua"
                  maxLength={6}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ""))}
                  placeholder="______"
                  type="text"
                  value={otpCode}
                />
              </div>

              <Button
                className="w-full"
                loading={isVerifying}
                onClick={async () => {
                  if (otpCode.length !== 6) {
                    setOtpError("Vui lòng nhập đủ 6 chữ số.");
                    return;
                  }
                  setOtpError(null);
                  setOtpSuccess(null);
                  setIsVerifying(true);
                  try {
                    const res = await verifyEmail({ email: otpEmail, otpCode });
                    setOtpSuccess(res.message || "Xác thực thành công!");
                    setTimeout(() => {
                      setShowOtpModal(false);
                      setMode("signin");
                      setSuccessMessage("Email đã xác thực. Bạn có thể đăng nhập ngay.");
                    }, 800);
                  } catch (err) {
                    setOtpError(getApiErrorMessage(err, "Mã OTP không đúng. Vui lòng thử lại."));
                  } finally {
                    setIsVerifying(false);
                  }
                }}
                type="button"
              >
                Xác thực
              </Button>

              <button
                className="font-sans text-sm text-gray transition hover:text-black-blue disabled:opacity-50"
                disabled={isResending}
                onClick={async () => {
                  setIsResending(true);
                  setOtpError(null);
                  try {
                    await resendOtp(otpEmail);
                    setOtpSuccess("Đã gửi lại mã OTP. Kiểm tra hộp thư của bạn.");
                  } catch (err) {
                    setOtpError(getApiErrorMessage(err, "Không thể gửi lại OTP. Thử lại sau."));
                  } finally {
                    setIsResending(false);
                  }
                }}
                type="button"
              >
                {isResending ? "Đang gửi..." : "Gửi lại mã OTP"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}
