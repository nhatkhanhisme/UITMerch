import { useEffect, useRef } from "react";
import { HomeEnd } from "../components/home/HomeEnd";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { HomeFixedChrome } from "../components/home/HomeFixedChrome";
import { HomeHero } from "../components/home/HomeHero";
import { HomeItem } from "../components/home/HomeItem";
import { HomeOrgan } from "../components/home/HomeOrgan";
import { HomeWhy } from "../components/home/HomeWhy";

export function HomePage() {
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const activeSectionIndexRef = useRef(0);
  const isSnappingRef = useRef(false);
  const snapTimeoutRef = useRef<number | null>(null);

  useEffect(() => {
    document.documentElement.classList.add("home-hide-native-scrollbar");
    document.body.classList.add("home-hide-native-scrollbar");

    return () => {
      document.documentElement.classList.remove("home-hide-native-scrollbar");
      document.body.classList.remove("home-hide-native-scrollbar");
    };
  }, []);

  useEffect(() => {
    const sectionIds = ["home-hero", "home-item", "home-organ", "home-end"];
    const sections = sectionIds
      .map((id) => document.getElementById(id))
      .filter((section): section is HTMLElement => Boolean(section));

    if (sections.length === 0) {
      return undefined;
    }

    const scrollRoot = scrollContainerRef.current;
    if (!scrollRoot) {
      return undefined;
    }

    const updateHash = (id: string) => {
      if (window.location.hash === `#${id}`) {
        return;
      }

      const nextUrl = `${window.location.pathname}${window.location.search}#${id}`;
      window.history.replaceState(null, "", nextUrl);
    };

    const snapToIndex = (nextIndex: number) => {
      const clampedIndex = Math.max(
        0,
        Math.min(nextIndex, sections.length - 1),
      );
      const targetSection = sections[clampedIndex];

      if (!targetSection || clampedIndex === activeSectionIndexRef.current) {
        return;
      }

      isSnappingRef.current = true;
      activeSectionIndexRef.current = clampedIndex;
      updateHash(targetSection.id);
      targetSection.scrollIntoView({ behavior: "smooth", block: "start" });

      if (snapTimeoutRef.current !== null) {
        window.clearTimeout(snapTimeoutRef.current);
      }

      snapTimeoutRef.current = window.setTimeout(() => {
        isSnappingRef.current = false;
      }, 750);
    };

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const index = sections.indexOf(entry.target as HTMLElement);

          if (index < 0) {
            return;
          }

          if (entry.isIntersecting && entry.intersectionRatio >= 0.55) {
            activeSectionIndexRef.current = index;
            updateHash(sections[index].id);
          }
        });
      },
      {
        threshold: [0.25, 0.55, 0.75],
        root: scrollRoot,
      },
    );

    sections.forEach((section) => {
      observer.observe(section);
    });

    const onWheel = (event: WheelEvent) => {
      event.preventDefault();

      if (isSnappingRef.current) {
        return;
      }

      const direction = event.deltaY > 0 ? 1 : -1;
      snapToIndex(activeSectionIndexRef.current + direction);
    };

    scrollRoot.addEventListener("wheel", onWheel, { passive: false });

    if (window.location.hash) {
      const initialIndex = sections.findIndex(
        (section) => `#${section.id}` === window.location.hash,
      );

      if (initialIndex >= 0) {
        activeSectionIndexRef.current = initialIndex;
        window.requestAnimationFrame(() => {
          sections[initialIndex].scrollIntoView({
            behavior: "auto",
            block: "start",
          });
        });
      }
    }

    return () => {
      observer.disconnect();
      scrollRoot.removeEventListener("wheel", onWheel);

      if (snapTimeoutRef.current !== null) {
        window.clearTimeout(snapTimeoutRef.current);
      }
    };
  }, []);

  return (
    <>
      <HomeFixedChrome />
      <div
        ref={scrollContainerRef}
        className="home-scroll-snap-container relative h-[100svh] overflow-y-auto overflow-x-hidden"
      >
        <HomeHero />
        <div className="relative isolate">
          <AmbientBackgroundGradients />

          <HomeItem />
          <HomeOrgan />
          {/* <HomeWhy /> */}
          <HomeEnd />
        </div>
      </div>
    </>
  );
}
