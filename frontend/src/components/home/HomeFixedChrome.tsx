import { useEffect, useRef, useState } from "react";
import { SlideBar } from "./SlideBar";
import { TopNavBar } from "./TopNavBar";

type HomeFixedChromeProps = {
  showSlideBar?: boolean;
};

export function HomeFixedChrome({ showSlideBar = true }: HomeFixedChromeProps) {
  const [scale, setScale] = useState(1);
  const [viewportWidth, setViewportWidth] = useState(1440);
  const [activeIndex, setActiveIndex] = useState<0 | 1 | 2 | 3>(0);
  const visibilityRef = useRef(new Map<number, number>());

  useEffect(() => {
    const updateScale = () => {
      setViewportWidth(window.innerWidth);
      setScale(Math.min(window.innerWidth / 1440, 1));
    };

    updateScale();
    window.addEventListener("resize", updateScale);

    return () => window.removeEventListener("resize", updateScale);
  }, []);

  useEffect(() => {
    if (!showSlideBar) {
      return undefined;
    }

    const sectionIds = ["home-hero", "home-item", "home-organ", "home-end"];
    const sections = sectionIds
      .map((id) => document.querySelector(`[data-section="${id}"]`))
      .filter((node): node is Element => Boolean(node));

    if (sections.length === 0) {
      return undefined;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const index = sections.indexOf(entry.target);
          if (index >= 0) {
            visibilityRef.current.set(index, entry.intersectionRatio);
          }
        });

        let nextIndex = 0;
        let maxRatio = -1;

        visibilityRef.current.forEach((ratio, index) => {
          if (ratio > maxRatio) {
            maxRatio = ratio;
            nextIndex = index;
          }
        });

        setActiveIndex(nextIndex as 0 | 1 | 2 | 3);
      },
      {
        threshold: [0, 0.2, 0.4, 0.6, 0.8, 1],
        rootMargin: "-15% 0px -30% 0px",
      },
    );

    sections.forEach((section, index) => {
      visibilityRef.current.set(index, 0);
      observer.observe(section);
    });

    return () => observer.disconnect();
  }, [showSlideBar]);

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
          transformOrigin: "top center",
        }}
      >
        <TopNavBar />
      </div>

      {showSlideBar && activeIndex !== 1 && (
        <div
          // FIX: Bug2
          className="pointer-events-none fixed right-0 z-20 hidden sm:right-8 lg:block"
          style={{
            top: "50%",
            transform: `translateY(-50%) scale(${scale})`,
            transformOrigin: "right center",
          }}
        >
          <SlideBar activeIndex={activeIndex} />
        </div>
      )}
    </>
  );
}
