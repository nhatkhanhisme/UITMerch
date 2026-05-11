import { useEffect } from "react";
import { HomeEnd } from "../components/home/HomeEnd";
import { AmbientBackgroundGradients } from "../components/home/AmbientBackgroundGradients";
import { HomeFixedChrome } from "../components/home/HomeFixedChrome";
import { HomeHero } from "../components/home/HomeHero";
import { HomeItem } from "../components/home/HomeItem";
import { HomeOrgan } from "../components/home/HomeOrgan";
import { HomeWhy } from "../components/home/HomeWhy";

export function HomePage() {
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

    const visibilityMap = new Map<string, number>();

    const updateHash = (id: string) => {
      if (window.location.hash === `#${id}`) {
        return;
      }

      const nextUrl = `${window.location.pathname}${window.location.search}#${id}`;
      window.history.replaceState(null, "", nextUrl);
    };

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.target.id) {
            return;
          }

          visibilityMap.set(entry.target.id, entry.intersectionRatio);
        });

        let activeId = sections[0].id;
        let maxRatio = -1;

        sections.forEach((section) => {
          const ratio = visibilityMap.get(section.id) ?? 0;
          if (ratio > maxRatio) {
            maxRatio = ratio;
            activeId = section.id;
          }
        });

        updateHash(activeId);
      },
      {
        threshold: [0, 0.2, 0.4, 0.6, 0.8, 1],
        rootMargin: "-20% 0px -20% 0px",
      },
    );

    sections.forEach((section) => {
      visibilityMap.set(section.id, 0);
      observer.observe(section);
    });

    return () => observer.disconnect();
  }, []);

  return (
    <>
      <HomeFixedChrome />
      <HomeHero />
      <div className="relative isolate">
        <AmbientBackgroundGradients />

        <HomeItem />
        <HomeOrgan />
        {/* <HomeWhy /> */}
        <HomeEnd />
      </div>
    </>
  );
}
