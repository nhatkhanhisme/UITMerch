import type { ButtonHTMLAttributes } from "react";

interface ScrollDownButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {}

const scrollButtonUrl = "/assets/figma/scroll-button.svg";
const scrollArrowUrl = "/assets/figma/scroll-arrow.svg";

export function ScrollDownButton({
  className = "",
  type = "button",
  ...props
}: ScrollDownButtonProps) {
  return (
    <button
      aria-label="Scroll down"
      className={["relative h-[74px] w-[73.408px]", className].join(" ")}
      data-node-id="17:4959"
      type={type}
      {...props}
    >
      <img
        alt=""
        className="absolute left-[-48.151px] top-[-47.389px] h-[148px] w-[147.744px] max-w-none"
        src={scrollButtonUrl}
      />
      <img
        alt=""
        className="absolute left-[21.912px] top-[36.704px] h-[14.652px] w-[29.305px] max-w-none"
        src={scrollArrowUrl}
      />
    </button>
  );
}
