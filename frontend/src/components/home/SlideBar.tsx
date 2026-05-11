import type { CSSProperties } from "react";
import FluidGlass from "./FluidGlass";

type SlideBarProps = {
  activeIndex?: 0 | 1 | 2 | 3;
};

const circlePositions = [0, 159, 318, 477];
const connectorPositions = [62, 221, 380];
const activePositions = [17, 176, 335, 494];

const connectorShellStyle: CSSProperties = {
  width: 70,
  height: 128,
  position: "absolute",
  filter:
    "drop-shadow(0 8px 18px rgba(82,128,145,0.1)) drop-shadow(0 0 10px rgba(255,255,255,0.18))",
};

const connectorPath =
  "M1 0 C11 11 20 22 20 39 L20 89 C20 106 11 117 1 128 L69 128 C59 117 50 106 50 89 L50 39 C50 22 59 11 69 0 Z";

const circleStyle: CSSProperties = {
  width: 80,
  height: 80,
  position: "absolute",
  background: "rgba(255,255,255,0.22)",
  border: "1px solid rgba(255,255,255,0.9)",
  boxShadow: `
    inset 2px 2px 8px rgba(255,255,255,1),
    inset -1px -1px 4px rgba(255,255,255,0.5),
    0 0 10px rgba(255,255,255,0.12)
  `,
  borderRadius: 9999,
  backdropFilter: "blur(6px)",
};

const activeCircleStyle: CSSProperties = {
  width: 46,
  height: 46,
  left: 18,
  position: "absolute",
  background: "rgba(87, 112, 122, 0.22)",
  border: "1px solid rgba(255,255,255,0.9)",
  boxShadow: `
    inset 2px 2px 8px rgba(255,255,255,1),
    inset -1px -1px 4px rgba(59,82,91,0.18),
    0 0 12px rgba(255,255,255,0.22)
  `,
  borderRadius: 9999,
  backdropFilter: "blur(6px)",
  transition: "top 360ms cubic-bezier(0.22, 1, 0.36, 1)",
  willChange: "top, transform",
  transform: "translateZ(0)",
};

export function SlideBar({ activeIndex = 0 }: SlideBarProps) {
  return (
    <div
      className="relative h-[557px] w-[81px]"
      data-node-id="38:12325"
      data-name="slide bar"
    >
      <div className="pointer-events-none absolute inset-0 overflow-hidden rounded-[40px]">
        <FluidGlass
          mode="lens"
          lensProps={{
            ior: 1.14,
            thickness: 5,
            chromaticAberration: 0.08,
            anisotropy: 0.01,
            scale: 0.14,
          }}
        />
      </div>

      {connectorPositions.map((top) => (
        <svg
          key={top}
          aria-hidden="true"
          viewBox="0 0 70 128"
          style={{
            ...connectorShellStyle,
            left: top === 380 ? 4 : 5,
            top: top - 5,
          }}
        >
          <defs>
            <linearGradient
              id={`connector-fill-${top}`}
              x1="0"
              x2="1"
              y1="0"
              y2="0"
            >
              <stop offset="0%" stopColor="rgba(255,255,255,0.08)" />
              <stop offset="24%" stopColor="rgba(255,255,255,0.2)" />
              <stop offset="50%" stopColor="rgba(255,255,255,0.3)" />
              <stop offset="76%" stopColor="rgba(255,255,255,0.2)" />
              <stop offset="100%" stopColor="rgba(255,255,255,0.08)" />
            </linearGradient>
          </defs>
          <path
            d={connectorPath}
            fill={`url(#connector-fill-${top})`}
            stroke="rgba(255,255,255,0.9)"
            strokeWidth="1.25"
          />
        </svg>
      ))}

      {circlePositions.map((top, index) => (
        <div
          key={top}
          style={{
            ...circleStyle,
            left: index === 3 ? 0 : 1,
            top,
            zIndex: 1,
          }}
        />
      ))}

      <div
        data-active-index={activeIndex}
        style={{
          ...activeCircleStyle,
          top: activePositions[activeIndex],
          zIndex: 2,
        }}
      />
    </div>
  );
}
