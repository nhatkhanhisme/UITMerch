import type { InputHTMLAttributes } from "react";
import { useState } from "react";
import type { InputBaseProps } from "../../types/ui";

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
            className="absolute right-4 top-1/2 -translate-y-1/2 font-sans text-sm text-gray transition hover:text-black-blue"
            onClick={() => setShowPassword((current) => !current)}
            type="button"
          >
            {showPassword ? "Hide" : "Show"}
          </button>
        ) : null}
      </div>
      {errorMessage ? (
        <p className="font-sans text-sm text-peach">{errorMessage}</p>
      ) : null}
    </div>
  );
}
