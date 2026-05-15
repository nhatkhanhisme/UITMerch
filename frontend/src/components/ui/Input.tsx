import type { InputBaseProps } from "../../types/shared";
import type { InputHTMLAttributes } from "react";
import { useState } from "react";

interface InputProps
  extends InputBaseProps,
    Omit<InputHTMLAttributes<HTMLInputElement>, "type" | "placeholder"> {}

export function Input({
  className = "",
  errorMessage,
  id,
  label,
  placeholder,
  type = "text",
  ...props
}: InputProps) {
  const [showPassword, setShowPassword] = useState(false);
  const inputId = id ?? props.name;
  const hasError = Boolean(errorMessage);
  const resolvedType = type === "password" && showPassword ? "text" : type;

  return (
    <div className="flex w-full flex-col gap-2">
      {label ? (
        <label className="font-sans text-sm text-gray" htmlFor={inputId}>
          {label}
        </label>
      ) : null}
      <div className="relative">
        <input
          className={[
            "h-12 w-full rounded-glass border bg-white/20 px-5 font-sans text-base text-ink shadow-glass transition duration-200",
            "placeholder:text-gray/70 focus:border-aqua focus:outline-none focus:ring-2 focus:ring-aqua/40",
            hasError ? "border-peach focus:border-peach focus:ring-peach/30" : "border-white/40",
            type === "password" ? "pr-24" : "",
            className
          ].join(" ")}
          id={inputId}
          placeholder={placeholder}
          type={resolvedType}
          {...props}
        />
        {type === "password" ? (
          <button
            aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            className="absolute right-4 top-1/2 -translate-y-1/2 text-gray transition hover:text-black-blue focus:outline-none"
            onClick={() => setShowPassword((current) => !current)}
            type="button"
          >
            {showPassword ? (
              /* Eye-off icon */
              <svg aria-hidden="true" className="size-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 3l18 18M10.477 10.477A3 3 0 0013.5 13.5M6.343 6.343A9.953 9.953 0 003 12c1.657 3.314 5.274 6 9 6a9.953 9.953 0 005.657-1.757M9.879 4.879A9.953 9.953 0 0112 4.5c3.726 0 7.343 2.686 9 6a10.06 10.06 0 01-1.384 2.377" />
              </svg>
            ) : (
              /* Eye icon */
              <svg aria-hidden="true" className="size-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.964-7.178z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            )}
          </button>
        ) : null}
      </div>
      {errorMessage ? (
        <p className="font-sans text-sm text-peach">{errorMessage}</p>
      ) : null}
    </div>
  );
}
