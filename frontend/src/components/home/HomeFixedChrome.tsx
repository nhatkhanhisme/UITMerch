import { useEffect, useState } from "react";
import { SlideBar } from "./SlideBar";
import { TopNavBar } from "./TopNavBar";

export function HomeFixedChrome() {
  const [scale, setScale] = useState(1);
  const [viewportWidth, setViewportWidth] = useState(1440);

  useEffect(() => {
    const updateScale = () => {
      setViewportWidth(window.innerWidth);
      setScale(Math.min(window.innerWidth / 1440, 1));
    };

    updateScale();
    window.addEventListener("resize", updateScale);

    return () => window.removeEventListener("resize", updateScale);
  }, []);

  return (
    <>
      <div
        className="fixed left-1/2 top-4 z-50 w-[calc(100vw-32px)] max-w-[1280.41px] -translate-x-1/2 sm:top-[18px] sm:w-[calc(100vw-64px)] lg:w-[1280.41px]"
        style={{
          // RESPONSIVE
          transform:
            viewportWidth >= 1024 && scale < 1
              ? `translateX(-50%) scale(${scale})`
              : "translateX(-50%)",
          transformOrigin: "top center"
        }}
      >
        <TopNavBar />
      </div>

      <div
        // FIX: Bug2
        className="pointer-events-none fixed right-0 z-20 hidden sm:right-4 lg:block"
        style={{
          top: 233 * scale,
          transform: `scale(${scale})`,
          transformOrigin: "top right"
        }}
      >
        <SlideBar activeIndex={0} />
      </div>
    </>
  );
}
