import type { CSSProperties } from "react";

type SlideBarProps = {
  activeIndex?: 0 | 1 | 2 | 3;
};

const circlePositions = [0, 159, 318, 477];
const connectorPositions = [62, 221, 380];
const activePositions = [17, 176, 335, 494];

const connectorStyle: CSSProperties = {
  width: 70,
  height: 118,
  position: "absolute",
  background: "rgba(255, 255, 255, 0.08)",
  backgroundBlendMode: "plus-lighter",
  boxShadow:
    "0.929px 0.866px 4px rgba(255, 255, 255, 0.13) inset, 1.858px 1.732px 8px rgba(255, 255, 255, 0.13) inset, -11.15px -10.392px 48px rgba(0, 0, 0, 0.15)",
  backdropFilter: "blur(2px)",
};

const circleStyle: CSSProperties = {
  width: 80,
  height: 80,
  position: "absolute",
  background: "rgba(255, 255, 255, 0.08)",
  backgroundBlendMode: "plus-lighter",
  boxShadow:
    "1.319px 1.23px 4.84px rgba(255, 255, 255, 0.13) inset, 2.249px 2.096px 9.68px rgba(255, 255, 255, 0.13) inset, -11.15px -10.392px 48px -12px rgba(0, 0, 0, 0.15)",
  borderRadius: 9999,
  outline: "1px solid rgba(255, 255, 255, 0.95)",
  backdropFilter: "blur(9.56px)",
};

const activeCircleStyle: CSSProperties = {
  width: 46,
  height: 46,
  left: 18,
  position: "absolute",
  background:
    "radial-gradient(circle at 50% 50%, rgba(71, 85, 105, 0.38) 0%, rgba(71, 85, 105, 0.18) 38%, rgba(255, 255, 255, 0.02) 72%)",
  backgroundBlendMode: "plus-lighter",
  boxShadow:
    "0.929px 0.866px 4px rgba(255, 255, 255, 0.09) inset, 1.858px 1.732px 8px rgba(255, 255, 255, 0.09) inset, -1.858px -1.732px 12px -8px rgba(0, 0, 0, 0.15), -11.15px -10.392px 48px -12px rgba(0, 0, 0, 0.15)",
  borderRadius: 9999,
  outline: "1px solid rgba(255, 255, 255, 0.95)",
  backdropFilter: "blur(2px)",
};

export function SlideBar({ activeIndex = 0 }: SlideBarProps) {
  return (
    <div
      className="relative h-[557px] w-[81px]"
      data-node-id="38:12325"
      data-name="slide bar"
    >
      {connectorPositions.map((top) => (
        <div
          key={top}
          style={{
            ...connectorStyle,
            left: top === 380 ? 4 : 5,
            top,
          }}
        />
      ))}

      {circlePositions.map((top, index) => (
        <div
          key={top}
          style={{
            ...circleStyle,
            left: index === 3 ? 0 : 1,
            top,
          }}
        />
      ))}

      <div
        data-active-index={activeIndex}
        style={{
          ...activeCircleStyle,
          top: activePositions[activeIndex],
        }}
      />
    </div>
  );
}
